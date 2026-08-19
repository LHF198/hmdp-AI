-- 1.参数列表
-- 1.1.优惠券id
local voucherId = ARGV[1]
-- 1.2.用户id
local userId = ARGV[2]
-- 1.3.订单id
local orderId = ARGV[3]

-- 2.数据key
-- 2.1.库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2.订单key
local orderKey = 'seckill:order:' .. voucherId
-- 2.3.秒杀开始时间key（毫秒时间戳）
local beginKey = 'seckill:begin:' .. voucherId
-- 2.4.秒杀结束时间key（毫秒时间戳）
local endKey = 'seckill:end:' .. voucherId

-- 3.脚本业务
-- 3.0.判断是否在秒杀时间窗口内（key 缺失时放行，兼容旧数据）
local nowTime = tonumber(redis.call('time')[1]) * 1000 + math.floor(tonumber(redis.call('time')[2]) / 1000)
local beginTime = tonumber(redis.call('get', beginKey))
if(beginTime and nowTime < beginTime) then
    -- 尚未开始，返回3
    return 3
end
local endTime = tonumber(redis.call('get', endKey))
if(endTime and nowTime > endTime) then
    -- 已经结束，返回4
    return 4
end
-- 3.1.判断库存是否充足 get stockKey（key 缺失时 get 返回 false，tonumber(false)=nil，需 nil 守护避免 Lua 运行时错误）
local stock = tonumber(redis.call('get', stockKey))
if(not stock or stock <= 0) then
    -- 3.2.库存不足，返回1
    return 1
end
-- 3.2.判断用户是否下单 SISMEMBER orderKey userId
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3.存在，说明是重复下单，返回2
    return 2
end
-- 3.4.扣库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- 3.5.下单（保存用户）sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.6.发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0