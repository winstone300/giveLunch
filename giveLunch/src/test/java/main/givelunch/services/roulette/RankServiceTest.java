package main.givelunch.services.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.dto.rankDto.RankRebuildResultDto;
import main.givelunch.properties.RankProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RankServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisScript<Number> incrementScript;

    @Mock
    private RedisScript<List> topRanksScript;

    @Mock
    private RedisScript<List> rebuildScript;

    private RankService rankService;

    @BeforeEach
    void setUp() {
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.systemUTC(),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript
        );
    }

    @Test
    @DisplayName("increment() - 만료 정리와 증가를 단일 스크립트로 실행하고 결과를 반환")
    void increment_executesAtomicIncrementScript() {
        String foodName = "비빔밥";
        doReturn(3L).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(foodName),
                anyString(),
                anyString(),
                anyString()
        );

        RankEntryDto result = rankService.increment(foodName);

        ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> foodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nowCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cutoffCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).execute(
                eq(incrementScript),
                keyCaptor.capture(),
                foodCaptor.capture(),
                eventCaptor.capture(),
                nowCaptor.capture(),
                cutoffCaptor.capture()
        );

        assertThat(keyCaptor.getValue()).containsExactly(RankService.RANK_KEY, RankService.RANK_EVENT_KEY);
        assertThat(foodCaptor.getValue()).isEqualTo(foodName);
        assertThat(eventCaptor.getValue()).endsWith(":" + foodName);
        assertThat(nowCaptor.getValue()).matches("\\d+");
        assertThat(cutoffCaptor.getValue()).matches("\\d+");
        assertThat(Long.parseLong(nowCaptor.getValue()) - Long.parseLong(cutoffCaptor.getValue()))
                .isEqualTo(Duration.ofHours(24).toSeconds());
        assertThat(result).isEqualTo(new RankEntryDto(foodName, 3L));
    }

    @Test
    @DisplayName("increment() - 증가 스크립트가 null을 반환하면 예외")
    void increment_throwsWhenScriptReturnsNull() {
        doReturn(null).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("김치찌개"),
                anyString(),
                anyString(),
                anyString()
        );

        assertThatThrownBy(() -> rankService.increment("김치찌개"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("returned null");
    }

    @Test
    @DisplayName("increment() - 현재 시각과 retention 기반 cutoff를 증가 스크립트에 전달")
    void increment_passesNowAndCutoffToIncrementScript() {
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.fixed(fixedTime, ZoneOffset.UTC),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript
        );
        doReturn(1L).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("제육볶음"),
                anyString(),
                eq(Long.toString(fixedTime.getEpochSecond())),
                eq(Long.toString(fixedTime.minus(Duration.ofHours(24)).getEpochSecond()))
        );

        rankService.increment("제육볶음");

        long cutoffEpochSeconds = fixedTime.minus(Duration.ofHours(24)).getEpochSecond();
        verify(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("제육볶음"),
                anyString(),
                eq(Long.toString(fixedTime.getEpochSecond())),
                eq(Long.toString(cutoffEpochSeconds))
        );
    }

    @Test
    @DisplayName("getTopRanks() - limit이 1 미만이면 1로 보정하여 단일 스크립트로 조회")
    void getTopRanks_normalizesLimitWhenLessThanOne() {
        doReturn(List.of("라면", 3L)).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString(),
                eq("1")
        );

        List<RankEntryDto> result = rankService.getTopRanks(0);

        verify(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString(),
                eq("1")
        );
        assertThat(result).containsExactly(new RankEntryDto("라면", 3L));
    }

    @Test
    @DisplayName("getTopRanks() - 결과가 비어있으면 빈 리스트 반환")
    void getTopRanks_returnsEmptyListWhenNoResult() {
        doReturn(List.of()).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString(),
                eq("3")
        );

        List<RankEntryDto> result = rankService.getTopRanks(3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopRanks() - 스크립트 결과를 DTO로 변환")
    void getTopRanks_mapsScriptResultsToDto() {
        doReturn(List.of("우동", 0L, "돈까스", 7L)).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString(),
                eq("2")
        );

        List<RankEntryDto> result = rankService.getTopRanks(2);

        assertThat(result).containsExactly(
                new RankEntryDto("우동", 0L),
                new RankEntryDto("돈까스", 7L)
        );
    }

    @Test
    @DisplayName("getTopRanks() - 현재 시각과 limit을 조회 스크립트에 전달")
    void getTopRanks_passesCutoffAndLimitToTopScript() {
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.fixed(fixedTime, ZoneOffset.UTC),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript
        );
        long cutoffEpochSeconds = fixedTime.minus(Duration.ofHours(24)).getEpochSecond();
        doReturn(List.of("김치찌개", 5L)).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(Long.toString(cutoffEpochSeconds)),
                eq("5")
        );

        List<RankEntryDto> result = rankService.getTopRanks(5);

        assertThat(result).containsExactly(new RankEntryDto("김치찌개", 5L));
    }

    @Test
    @DisplayName("getTopRanks() - malformed 결과면 예외")
    void getTopRanks_throwsWhenScriptReturnsMalformedResults() {
        doReturn(List.of("우동")).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString(),
                eq("1")
        );

        assertThatThrownBy(() -> rankService.getTopRanks(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed");
    }

    @Test
    @DisplayName("rebuildRanks() - retention 창 기준 복구 결과를 반환")
    void rebuildRanks_returnsRebuildResult() {
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.fixed(fixedTime, ZoneOffset.UTC),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript
        );
        long cutoffEpochSeconds = fixedTime.minus(Duration.ofHours(24)).getEpochSecond();
        doReturn(List.of(3L, 2L)).when(redisTemplate).execute(
                eq(rebuildScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(Long.toString(cutoffEpochSeconds))
        );

        RankRebuildResultDto result = rankService.rebuildRanks();

        assertThat(result).isEqualTo(new RankRebuildResultDto(3L, 2L, cutoffEpochSeconds));
    }

    @Test
    @DisplayName("rebuildRanks() - malformed 결과면 예외")
    void rebuildRanks_throwsWhenScriptReturnsMalformedResults() {
        doReturn(List.of(1L)).when(redisTemplate).execute(
                eq(rebuildScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString()
        );

        assertThatThrownBy(() -> rankService.rebuildRanks())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed");
    }
}
