package main.givelunch.services.roulette;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.dto.rankDto.RankRebuildResultDto;
import main.givelunch.properties.RankProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RankService {
    static final String RANK_KEY = "roulette:food:rank";
    static final String RANK_EVENT_KEY = "roulette:food:rank:events";
    private static final Logger log = LoggerFactory.getLogger(RankService.class);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final long retentionSeconds;
    private final RedisScript<Long> incrementScript;
    private final RedisScript<List> topRanksScript;
    private final RedisScript<List> rebuildScript;

    @Autowired
    public RankService(StringRedisTemplate redisTemplate, RankProperties rankProperties) {
        this(
                redisTemplate,
                Clock.systemUTC(),
                rankProperties,
                createIncrementScript(),
                createTopRanksScript(),
                createRebuildScript()
        );
    }

    RankService(
            StringRedisTemplate redisTemplate,
            Clock clock,
            RankProperties rankProperties,
            RedisScript<Long> incrementScript,
            RedisScript<List> topRanksScript,
            RedisScript<List> rebuildScript) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.retentionSeconds = rankProperties.retention().toSeconds();
        this.incrementScript = incrementScript;
        this.topRanksScript = topRanksScript;
        this.rebuildScript = rebuildScript;
    }

    public RankEntryDto increment(String name) {
        long now = Instant.now(clock).getEpochSecond();
        long cutoff = now - retentionSeconds;
        Long score = redisTemplate.execute(
                incrementScript,
                List.of(RANK_KEY, RANK_EVENT_KEY),
                name,
                buildEventMember(name),
                Long.toString(now),
                Long.toString(cutoff)
        );

        return new RankEntryDto(name, requireScore(score, "increment"));
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

    public RankRebuildResultDto rebuildRanks() {
        long cutoff = Instant.now(clock).getEpochSecond() - retentionSeconds;
        log.info("Starting manual rank rebuild. cutoffEpochSeconds={}", cutoff);
        List<?> rawResults = redisTemplate.execute(
                rebuildScript,
                List.of(RANK_KEY, RANK_EVENT_KEY),
                Long.toString(cutoff)
        );
        List<Long> counts = toLongList(rawResults, "rebuild");
        RankRebuildResultDto result = new RankRebuildResultDto(counts.get(0), counts.get(1), cutoff);
        log.info(
                "Finished manual rank rebuild. rebuiltEventCount={}, rebuiltFoodCount={}, cutoffEpochSeconds={}",
                result.rebuiltEventCount(),
                result.rebuiltFoodCount(),
                result.cutoffEpochSeconds()
        );
        return result;
    }

    private static RedisScript<Long> createIncrementScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/rank-increment.lua"));
        script.setResultType(Long.class);
        return script;
    }

    private static RedisScript<List> createTopRanksScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/rank-top.lua"));
        script.setResultType(List.class);
        return script;
    }

    private static RedisScript<List> createRebuildScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/rank-rebuild.lua"));
        script.setResultType(List.class);
        return script;
    }


    private long requireScore(Long value, String operation) {
        if (value == null) {
            throw new IllegalStateException("Redis rank " + operation + " script returned null");
        }
        return value;
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
            entries.add(new RankEntryDto(name, parseLong(rawScore, "Redis rank top script returned non-numeric score")));
        }
        return entries;
    }

    private List<Long> toLongList(List<?> rawResults, String operation) {
        if (rawResults == null || rawResults.size() != 2) {
            throw new IllegalStateException("Redis rank " + operation + " script returned malformed results");
        }

        List<Long> values = new ArrayList<>(rawResults.size());
        for (Object rawResult : rawResults) {
            values.add(parseLong(rawResult, "Redis rank " + operation + " script returned non-numeric result"));
        }
        return values;
    }

    private long parseLong(Object rawValue, String errorMessage) {
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (rawValue instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(errorMessage, e);
            }
        }
        throw new IllegalStateException(errorMessage);
    }

    private String buildEventMember(String name) {
        return UUID.randomUUID() + ":" + name;
    }
}
