package main.givelunch.services.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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

    @Mock
    private RedisScript<Number> cleanupScript;

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
                rebuildScript,
                cleanupScript
        );
    }

    @Test
    @DisplayName("increment() - 증가 스크립트만 실행하고 결과를 반환")
    void increment_executesIncrementScriptOnly() {
        String foodName = "비빔밥";
        doReturn(3L).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(foodName),
                anyString(),
                anyString()
        );

        RankEntryDto result = rankService.increment(foodName);

        ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> foodCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nowCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).execute(
                eq(incrementScript),
                keyCaptor.capture(),
                foodCaptor.capture(),
                eventCaptor.capture(),
                nowCaptor.capture()
        );

        assertThat(keyCaptor.getValue()).containsExactly(RankService.RANK_KEY, RankService.RANK_EVENT_KEY);
        assertThat(foodCaptor.getValue()).isEqualTo(foodName);
        assertThat(eventCaptor.getValue()).endsWith(":" + foodName);
        assertThat(nowCaptor.getValue()).matches("\\d+");
        assertThat(result).isEqualTo(new RankEntryDto(foodName, 3L));
        verify(redisTemplate, never()).execute(eq(cleanupScript), any(), any());
    }

    @Test
    @DisplayName("increment() - 증가 스크립트가 null을 반환하면 예외")
    void increment_throwsWhenScriptReturnsNull() {
        doReturn(null).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("김치찌개"),
                anyString(),
                anyString()
        );

        assertThatThrownBy(() -> rankService.increment("김치찌개"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("returned null");
    }

    @Test
    @DisplayName("increment() - 현재 시각을 증가 스크립트에 전달")
    void increment_passesNowToIncrementScript() {
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.fixed(fixedTime, ZoneOffset.UTC),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript,
                cleanupScript
        );
        doReturn(1L).when(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("제육볶음"),
                anyString(),
                eq(Long.toString(fixedTime.getEpochSecond()))
        );

        rankService.increment("제육볶음");

        verify(redisTemplate).execute(
                eq(incrementScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq("제육볶음"),
                anyString(),
                eq(Long.toString(fixedTime.getEpochSecond()))
        );
    }

    @Test
    @DisplayName("getTopRanks() - limit이 1 미만이면 1로 보정하여 단일 스크립트로 조회")
    void getTopRanks_normalizesLimitWhenLessThanOne() {
        doReturn(List.of("라면", 3L)).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY)),
                eq("1")
        );

        List<RankEntryDto> result = rankService.getTopRanks(0);

        verify(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY)),
                eq("1")
        );
        verify(redisTemplate, never()).execute(eq(cleanupScript), any(), any());
        assertThat(result).containsExactly(new RankEntryDto("라면", 3L));
    }

    @Test
    @DisplayName("getTopRanks() - 결과가 비어있으면 빈 리스트 반환")
    void getTopRanks_returnsEmptyListWhenNoResult() {
        doReturn(List.of()).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY)),
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
                eq(List.of(RankService.RANK_KEY)),
                eq("2")
        );

        List<RankEntryDto> result = rankService.getTopRanks(2);

        assertThat(result).containsExactly(
                new RankEntryDto("우동", 0L),
                new RankEntryDto("돈까스", 7L)
        );
    }

    @Test
    @DisplayName("getTopRanks() - malformed 결과면 예외")
    void getTopRanks_throwsWhenScriptReturnsMalformedResults() {
        doReturn(List.of("우동")).when(redisTemplate).execute(
                eq(topRanksScript),
                eq(List.of(RankService.RANK_KEY)),
                eq("1")
        );

        assertThatThrownBy(() -> rankService.getTopRanks(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed");
    }

    @Test
    @DisplayName("cleanupExpiredRanks() - retention 기준 cutoff로 cleanup 스크립트를 실행")
    void cleanupExpiredRanks_runsCleanupScriptWithCutoff() {
        Instant fixedTime = Instant.parse("2026-01-02T00:00:00Z");
        RankProperties rankProperties = new RankProperties(Duration.ofHours(24));
        rankService = new RankService(
                redisTemplate,
                Clock.fixed(fixedTime, ZoneOffset.UTC),
                rankProperties,
                incrementScript,
                topRanksScript,
                rebuildScript,
                cleanupScript
        );
        long cutoffEpochSeconds = fixedTime.minus(Duration.ofHours(24)).getEpochSecond();
        doReturn(4L).when(redisTemplate).execute(
                eq(cleanupScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(Long.toString(cutoffEpochSeconds))
        );

        long removedCount = rankService.cleanupExpiredRanks();

        assertThat(removedCount).isEqualTo(4L);
        verify(redisTemplate).execute(
                eq(cleanupScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                eq(Long.toString(cutoffEpochSeconds))
        );
    }

    @Test
    @DisplayName("cleanupExpiredRanks() - null 반환이면 예외")
    void cleanupExpiredRanks_throwsWhenScriptReturnsNull() {
        doReturn(null).when(redisTemplate).execute(
                eq(cleanupScript),
                eq(List.of(RankService.RANK_KEY, RankService.RANK_EVENT_KEY)),
                anyString()
        );

        assertThatThrownBy(() -> rankService.cleanupExpiredRanks())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup");
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
                rebuildScript,
                cleanupScript
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
