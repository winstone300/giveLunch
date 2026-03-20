local rankKey = KEYS[1]
local eventKey = KEYS[2]
local foodName = ARGV[1]
local eventMember = ARGV[2]
local nowEpochSeconds = tonumber(ARGV[3])

redis.call('ZADD', eventKey, nowEpochSeconds, eventMember)
return tonumber(redis.call('ZINCRBY', rankKey, 1, foodName))
