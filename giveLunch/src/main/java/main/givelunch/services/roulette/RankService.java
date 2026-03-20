package main.givelunch.services.roulette;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.properties.RankProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RankService {
    static final String RANK_KEY = "roulette:food:rank";
    static final String RANK_EVENT_KEY = "roulette:food:rank:events";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final long retentionSeconds;
    private final RedisScript<Number> incrementScript;
    private final RedisScript<List> topRanksScript;

    @Autowired
    public RankService(StringRedisTemplate redisTemplate, RankProperties rankProperties) {
        this(
                redisTemplate,
                Clock.systemUTC(),
                rankProperties,
                createIncrementScript(),
                createTopRanksScript()
        );
    }

    RankService(
            StringRedisTemplate redisTemplate,
            Clock clock,
            RankProperties rankProperties,
            RedisScript<Number> incrementScript,
            RedisScript<List> topRanksScript) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.retentionSeconds = rankProperties.retention().toSeconds();
        this.incrementScript = incrementScript;
        this.topRanksScript = topRanksScript;
    }

    public RankEntryDto increment(String name) {
        long now = Instant.now(clock).getEpochSecond();
        long cutoff = now - retentionSeconds;
        Number score = redisTemplate.execute(
                incrementScript,
                List.of(RANK_KEY, RANK_EVENT_KEY),
                name,
                buildEventMember(name),
                Long.toString(now),
                Long.toString(cutoff)
        );

        return new RankEntryDto(name, toLong(score, "increment"));
    }

    public List<RankEntryDto> getTopRanks(int limit) {
        int safeLimit = Math.max(1, limit);
        long cutoff = Instant.now(clock).getEpochSecond() - retentionSeconds;
        List<?> rawResults = redisTemplate.execute(
                topRanksScript,
                List.of(RANK_KEY, RANK_EVENT_KEY),
                Long.toString(cutoff),
                Integer.toString(safeLimit)
        );
        return toRankEntries(rawResults);
    }

    private static RedisScript<Number> createIncrementScript() {
        DefaultRedisScript<Number> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/rank-increment.lua"));
        script.setResultType(Number.class);
        return script;
    }

    private static RedisScript<List> createTopRanksScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/rank-top.lua"));
        script.setResultType(List.class);
        return script;
    }

    private long toLong(Number value, String operation) {
        if (value == null) {
            throw new IllegalStateException("Redis rank " + operation + " script returned null");
        }
        return value.longValue();
    }

    private List<RankEntryDto> toRankEntries(List<?> rawResults) {
        if (rawResults == null || rawResults.isEmpty()) {
            return List.of();
        }
        if (rawResults.size() % 2 != 0) {
            throw new IllegalStateException("Redis rank top script returned malformed results");
        }

        List<RankEntryDto> entries = new ArrayList<>(rawResults.size() / 2);
        for (int i = 0; i < rawResults.size(); i += 2) {
            Object rawName = rawResults.get(i);
            Object rawScore = rawResults.get(i + 1);
            if (!(rawName instanceof String name)) {
                throw new IllegalStateException("Redis rank top script returned non-string name");
            }
            if (!(rawScore instanceof Number score)) {
                throw new IllegalStateException("Redis rank top script returned non-numeric score");
            }
            entries.add(new RankEntryDto(name, score.longValue()));
        }
        return entries;
    }

    private String buildEventMember(String name) {
        return UUID.randomUUID() + ":" + name;
    }
}
