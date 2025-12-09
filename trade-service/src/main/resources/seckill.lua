-- 商品ID和用户ID从参数传入
local itemId = ARGV[1]
local userId = ARGV[2]

-- 秒杀商品Hash的Redis键（存储库存、开始时间、结束时间）
local seckillItemKey = "seckill:item:" .. itemId
-- 记录用户下单的集合键（防止重复下单）
local orderKey = "seckill:order:" .. itemId

-- 1. 获取当前Redis服务器时间（转换为毫秒级时间戳，与存储的时间戳统一单位）
local now = redis.call('time')  -- 返回格式：{秒, 微秒}
local currentTime = now[1] * 1000 + math.floor(now[2] / 1000)  -- 毫秒级时间戳

-- 2. 从Hash中获取秒杀核心信息（库存、开始时间、结束时间）
local stock = tonumber(redis.call("hget", seckillItemKey, "stock"))
local beginTime = tonumber(redis.call("hget", seckillItemKey, "beginTime"))
local endTime = tonumber(redis.call("hget", seckillItemKey, "endTime"))

-- 3. 时间校验（判断是否在秒杀时间范围内）
if currentTime < beginTime then
    return -4  -- 秒杀未开始
end

if currentTime > endTime then
    return -3  -- 秒杀已结束
end

-- 4. 库存校验
if not stock or stock <= 0 then
    return -2  -- 库存不足
end

-- 5. 校验用户是否已下单（防止重复购买）
if redis.call("sismember", orderKey, userId) == 1 then
    return -1  -- 用户已下单
end

-- 6. 扣减库存（Hash中库存减1）
redis.call("hincrby", seckillItemKey, "stock", -1)

-- 7. 记录用户下单信息（添加到集合）
redis.call("sadd", orderKey, userId)

return 0  -- 秒杀成功