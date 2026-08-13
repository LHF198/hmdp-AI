package com.hmdp.utils;

/**
 * 秒杀模块常量：Lua 脚本返回码与 Redis Stream 基础设施命名。
 *
 * <p>返回码语义与 {@code seckill.lua} 中 return 值一一对应，修改脚本时必须同步本类；
 * Stream 名与 seckill.lua 中 XADD 目标保持一致，避免两端命名漂移导致消息丢失。
 */
public final class SeckillConstants {

    private SeckillConstants() {
    }

    // ==================== Lua 脚本返回码（对应 seckill.lua return 值） ====================

    /** 秒杀成功：已扣库存并投递订单消息 */
    public static final int SECKILL_SUCCESS = 0;
    /** 库存不足 */
    public static final int SECKILL_STOCK_NOT_ENOUGH = 1;
    /** 重复下单（一人一单） */
    public static final int SECKILL_REPEAT_ORDER = 2;
    /** 秒杀尚未开始 */
    public static final int SECKILL_NOT_BEGIN = 3;
    /** 秒杀已经结束 */
    public static final int SECKILL_ENDED = 4;

    // ==================== Redis Stream 基础设施（seckill.lua XADD 目标同源） ====================

    /** 秒杀订单 Stream（生产端 XADD 与消费端 XREADGROUP 共用） */
    public static final String STREAM_ORDERS = "stream.orders";
    /** 秒杀订单死信队列 Stream：多次处理失败的消息落盘于此，便于人工排查/重放 */
    public static final String STREAM_ORDERS_DLQ = "stream.orders.dlq";
    /** 秒杀订单消费者组 */
    public static final String STREAM_GROUP = "g1";
    /** 秒杀订单消费者 */
    public static final String STREAM_CONSUMER = "c1";
}
