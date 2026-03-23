local rankKey = KEYS[1]
local eventKey = KEYS[2]
local foodName = ARGV[1]
local eventMember = ARGV[2]
local nowEpochSeconds = tonumber(ARGV[3])
local cutoffEpochSeconds = tonumber(ARGV[4])

local expiredEvents = redis.call('ZRANGEBYSCORE', eventKey, '-inf', cutoffEpochSeconds)

if #expiredEvents > 0 then
    local decrementByFood = {}
    for _, expiredEvent in ipairs(expiredEvents) do
        local separatorIndex = string.find(expiredEvent, ':', 1, true)
        local expiredFoodName = expiredEvent
        if separatorIndex and separatorIndex < string.len(expiredEvent) then
            expiredFoodName = string.sub(expiredEvent, separatorIndex + 1)
        end
        decrementByFood[expiredFoodName] = (decrementByFood[expiredFoodName] or 0) + 1
    end

    for expiredFoodName, decrementCount in pairs(decrementByFood) do
        local updatedScore = tonumber(redis.call('ZINCRBY', rankKey, -decrementCount, expiredFoodName))
        if not updatedScore or updatedScore <= 0 then
            redis.call('ZREM', rankKey, expiredFoodName)
        end
    end

    redis.call('ZREM', eventKey, unpack(expiredEvents))
end

redis.call('ZADD', eventKey, nowEpochSeconds, eventMember)
return tonumber(redis.call('ZINCRBY', rankKey, 1, foodName))    
