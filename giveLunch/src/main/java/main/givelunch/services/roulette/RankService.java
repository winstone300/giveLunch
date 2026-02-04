package main.givelunch.services.roulette;

import java.util.List;
import java.util.Set;
import main.givelunch.dto.rankDto.RankEntryDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class RankService {
    private static final String RANK_KEY = "roulette:food:rank";

    private final StringRedisTemplate redisTemplate;

    public RankService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RankEntryDto increment(String name) {
        Double score = redisTemplate.opsForZSet().incrementScore(RANK_KEY, name, 1);
        long count = score == null ? 1L : score.longValue();
        return new RankEntryDto(name, count);
    }

    public List<RankEntryDto> getTopRanks(int limit) {
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
}