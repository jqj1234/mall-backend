-- 回滚秒杀操作的 Lua 脚本
-- 用途：
--   1. 将指定商品的库存（hash 字段 "stock"）加 1
--   2. 从记录已下单用户的 Set 中移除当前用户 ID

-- KEYS[1] = "seckill:item:{itemId}"     -> 商品库存 Hash Key
-- KEYS[2] = "seckill:order:{itemId}"    -> 已下单用户 Set Key
-- ARGV[1] = userId                      -> 要移除的用户 ID（字符串）

-- 增加库存
redis.call('HINCRBY', KEYS[1], 'stock', 1)

-- 删除用户 ID 记录
redis.call('SREM', KEYS[2], ARGV[1])