local rankKey = KEYS[1]
local limit = tonumber(ARGV[1])

return redis.call('ZREVRANGE', rankKey, 0, limit - 1, 'WITHSCORES')
