package main.givelunch.services.roulette;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.properties.RankProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class RankService {
    private static final String RANK_KEY = "roulette:food:rank";        // 총 개수 저장
    private static final String RANK_EVENT_KEY = "roulette:food:rank:events";   // 이벤트 발생 시점 저장

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final long retentionSeconds;

    @Autowired
    public RankService(StringRedisTemplate redisTemplate, RankProperties rankProperties) {
        this(redisTemplate, Clock.systemUTC(), rankProperties);
    }

    RankService(StringRedisTemplate redisTemplate, Clock clock, RankProperties rankProperties) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.retentionSeconds = rankProperties.retention().toSeconds();
    }

    public RankEntryDto increment(String name) {
        removeExpiredEvents();
        long now = Instant.now(clock).getEpochSecond();
        redisTemplate.opsForZSet().add(RANK_EVENT_KEY, buildEventMember(name), now);
        Double score = redisTemplate.opsForZSet().incrementScore(RANK_KEY, name, 1);
        long count = score == null ? 1L : score.longValue();
        return new RankEntryDto(name, count);
    }

    public List<RankEntryDto> getTopRanks(int limit) {
        removeExpiredEvents();
        int safeLimit = Math.max(1, limit);
        Set<ZSetOperations.TypedTuple<String>> results =
                redisTemplate.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, safeLimit - 1);
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(tuple -> new RankEntryDto(tuple.getValue(), tuple.getScore() == null ? 0L : tuple.getScore().longValue()))
                .toList();
    }

    private void removeExpiredEvents() {
        long cutoff = Instant.now(clock).getEpochSecond() - retentionSeconds;
        Set<String> expiredEvents = redisTemplate.opsForZSet().rangeByScore(RANK_EVENT_KEY, 0, cutoff);
        if (expiredEvents == null || expiredEvents.isEmpty()) {
            return;
        }

        redisTemplate.opsForZSet().remove(RANK_EVENT_KEY, expiredEvents.toArray());

        Map<String, Integer> decrementByFood = new HashMap<>();
        for (String event : expiredEvents) {
            decrementByFood.merge(extractFoodName(event), 1, Integer::sum);
        }

        decrementByFood.forEach((foodName, decrementCount) -> {
            Double score = redisTemplate.opsForZSet().incrementScore(RANK_KEY, foodName, -decrementCount);
            if (score == null || score <= 0) {
                redisTemplate.opsForZSet().remove(RANK_KEY, foodName);
            }
        });
    }

    private String buildEventMember(String name) {
        return UUID.randomUUID() + ":" + name;
    }

    private String extractFoodName(String eventMember) {
        int separatorIndex = eventMember.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == eventMember.length() - 1) {
            return eventMember;
        }
        return eventMember.substring(separatorIndex + 1);
    }
}