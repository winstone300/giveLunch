local rankKey = KEYS[1]
local eventKey = KEYS[2]
local cutoffEpochSeconds = tonumber(ARGV[1])

redis.call('ZREMRANGEBYSCORE', eventKey, '-inf', cutoffEpochSeconds)

local recentEvents = redis.call('ZRANGEBYSCORE', eventKey, '(' .. cutoffEpochSeconds, '+inf')
local eventCount = #recentEvents

local countsByFood = {}
for _, eventMember in ipairs(recentEvents) do
    local separatorIndex = string.find(eventMember, ':', 1, true)
    local foodName = eventMember
    if separatorIndex and separatorIndex < string.len(eventMember) then
        foodName = string.sub(eventMember, separatorIndex + 1)
    end
    countsByFood[foodName] = (countsByFood[foodName] or 0) + 1
end

redis.call('DEL', rankKey)

local zaddArgs = {}
local foodCount = 0
for foodName, count in pairs(countsByFood) do
    foodCount = foodCount + 1
    table.insert(zaddArgs, count)
    table.insert(zaddArgs, foodName)
end

if #zaddArgs > 0 then
    redis.call('ZADD', rankKey, unpack(zaddArgs))
end

return {eventCount, foodCount}
