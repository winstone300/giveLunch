package main.givelunch.services.roulette;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.properties.RankProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RankServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RankService rankService;

    @BeforeEach
    void setUp() {
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(redisTemplate, Clock.systemUTC(), rankProperties);
    }

    private void stubNoExpiredEvents() {
        when(zSetOperations.rangeByScore(eq("roulette:food:rank:events"), anyDouble(), anyDouble()))
                .thenReturn(Set.of());
    }

    @Test
    @DisplayName("increment() - redis score가 null이면 1로 처리")
    void increment_returnsOneWhenRedisScoreIsNull() {
        // given
        String foodName = "비빔밥";
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        stubNoExpiredEvents();
        when(zSetOperations.incrementScore("roulette:food:rank", foodName, 1)).thenReturn(null);

        // when
        RankEntryDto result = rankService.increment(foodName);

        // then
        assertThat(result.name()).isEqualTo(foodName);
        assertThat(result.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("increment() - redis score를 long으로 변환해 반환")
    void increment_returnsScoreAsLong() {
        // given
        String foodName = "김치찌개";
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        stubNoExpiredEvents();
        when(zSetOperations.incrementScore("roulette:food:rank", foodName, 1)).thenReturn(5.0);

        // when
        RankEntryDto result = rankService.increment(foodName);

        // then
        assertThat(result.name()).isEqualTo(foodName);
        assertThat(result.count()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getTopRanks() - limit이 1 미만이면 1로 보정하여 조회")
    void getTopRanks_normalizesLimitWhenLessThanOne() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        stubNoExpiredEvents();
        when(zSetOperations.reverseRangeWithScores(eq("roulette:food:rank"), anyLong(), anyLong()))
                .thenReturn(Set.of(new DefaultTypedTuple<>("라면", 3.0)));

        // when
        List<RankEntryDto> result = rankService.getTopRanks(0);

        // then
        verify(zSetOperations).reverseRangeWithScores("roulette:food:rank", 0, 0);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("라면");
        assertThat(result.get(0).count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getTopRanks() - 결과가 비어있으면 빈 리스트 반환")
    void getTopRanks_returnsEmptyListWhenNoResult() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        stubNoExpiredEvents();
        when(zSetOperations.reverseRangeWithScores("roulette:food:rank", 0, 2)).thenReturn(null);

        // when
        List<RankEntryDto> result = rankService.getTopRanks(3);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopRanks() - score null은 0으로 변환")
    void getTopRanks_convertsNullScoreToZero() {
        // given
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("우동", null));
        tuples.add(new DefaultTypedTuple<>("돈까스", 7.0));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        stubNoExpiredEvents();
        when(zSetOperations.reverseRangeWithScores("roulette:food:rank", 0, 1)).thenReturn(tuples);

        // when
        List<RankEntryDto> result = rankService.getTopRanks(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("우동");
        assertThat(result.get(0).count()).isEqualTo(0L);
        assertThat(result.get(1).name()).isEqualTo("돈까스");
        assertThat(result.get(1).count()).isEqualTo(7L);
    }

    @Test
    @DisplayName("increment()요청시 24시간 지난 이벤트를 제거하고 점수를 감소시킨다")
    void increment_removesExpiredEventsBeforeScoring() {
        // given
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(redisTemplate, Clock.fixed(fixedTime, ZoneOffset.UTC), rankProperties);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        long cutoffEpochSeconds = fixedTime.minus(Duration.ofHours(24)).getEpochSecond();
        when(zSetOperations.rangeByScore("roulette:food:rank:events", 0, cutoffEpochSeconds))
                .thenReturn(Set.of("uuid-1:김밥", "uuid-2:김밥", "uuid-3:라면"));
        when(zSetOperations.incrementScore("roulette:food:rank", "김밥", -2)).thenReturn(0.0);
        when(zSetOperations.incrementScore("roulette:food:rank", "라면", -1)).thenReturn(4.0);
        when(zSetOperations.incrementScore("roulette:food:rank", "제육볶음", 1)).thenReturn(3.0);

        // when
        RankEntryDto result = rankService.increment("제육볶음");

        // then
        ArgumentCaptor<Object> expiredMemberCaptor = ArgumentCaptor.forClass(Object.class);
        verify(zSetOperations).remove(
                eq("roulette:food:rank:events"),
                expiredMemberCaptor.capture(),
                expiredMemberCaptor.capture(),
                expiredMemberCaptor.capture()
        );
        assertThat(expiredMemberCaptor.getAllValues())
                .containsExactlyInAnyOrder("uuid-1:김밥", "uuid-2:김밥", "uuid-3:라면");
        verify(zSetOperations).incrementScore("roulette:food:rank", "김밥", -2);
        verify(zSetOperations).remove("roulette:food:rank", "김밥");
        verify(zSetOperations).incrementScore("roulette:food:rank", "라면", -1);
        assertThat(result.count()).isEqualTo(3L);
    }
}