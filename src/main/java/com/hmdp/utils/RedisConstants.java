package com.hmdp.utils;

/**
 * Redis 键名与过期时间常量。注意：TTL 数值本身不带单位，实际单位以使用处的 TimeUnit 为准 （除 CACHE_TYPE_TTL
 * 为小时外，其余均为分钟），详见各常量注释。
 */
public class RedisConstants {

    /**
     * 短信验证码 key 前缀，完整键：login:code:{phone}
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    /**
     * 短信验证码有效期：2 分钟
     */
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * 登录用户 token key 前缀，完整键：login:token:{token}
     */
    public static final String LOGIN_USER_KEY = "login:token:";
    /**
     * 登录用户会话有效期：36000 分钟（约 25 天）
     */
    public static final Long LOGIN_USER_TTL = 36000L;

    /**
     * 密码登录失败计数 key 前缀，完整键：login:fail:{phone}（String，防暴力破解）
     */
    public static final String LOGIN_FAIL_KEY = "login:fail:";
    /**
     * 密码登录失败锁定窗口：10 分钟
     */
    public static final Long LOGIN_FAIL_TTL = 10L;

    /**
     * 空值占位缓存的有效期：2 分钟（防缓存穿透）
     */
    public static final Long CACHE_NULL_TTL = 2L;

    /**
     * 店铺缓存有效期：30 分钟
     */
    public static final Long CACHE_SHOP_TTL = 30L;
    /**
     * 店铺缓存 key 前缀，完整键：cache:shop:{shopId}
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    /**
     * 店铺重建互斥锁 key 前缀，完整键：lock:shop:{shopId}
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    /**
     * 店铺重建互斥锁有效期：10 秒（见 CacheClient.tryLock）
     */
    public static final Long LOCK_SHOP_TTL = 10L;
    /**
     * 店铺类型全量缓存键（单 key，无后缀）
     */
    public static final String CACHE_TYPE_KEY = "cache:type";
    /**
     * 店铺类型缓存有效期：24 小时
     */
    public static final Long CACHE_TYPE_TTL = 24L;

    /**
     * 秒杀库存 key 前缀，完整键：seckill:stock:{voucherId}（String）
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 秒杀已下单用户集合 key 前缀，完整键：seckill:order:{voucherId}（Set）
     */
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    /**
     * 秒杀开始时间 key 前缀，完整键：seckill:begin:{voucherId}（时间戳毫秒）
     */
    public static final String SECKILL_BEGIN_KEY = "seckill:begin:";
    /**
     * 秒杀结束时间 key 前缀，完整键：seckill:end:{voucherId}（时间戳毫秒）
     */
    public static final String SECKILL_END_KEY = "seckill:end:";
    /**
     * 笔记点赞用户集合 key 前缀，完整键：blog:liked:{blogId}（ZSet）
     */
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    /**
     * 用户 Feed 收件箱 key 前缀，完整键：feed:{userId}（ZSet，score=时间戳）
     */
    public static final String FEED_KEY = "feed:";
    /**
     * 店铺地理位置 key 前缀，完整键：shop:geo:{typeId}（GEO）
     */
    public static final String SHOP_GEO_KEY = "shop:geo:";
    /**
     * 用户签到 bitMap key 前缀，完整键：sign:{userId}:{yyyyMM}
     */
    public static final String USER_SIGN_KEY = "sign:";
    /**
     * 秒杀下单分布式锁 key 前缀，完整键：lock:order:{userId}（Redisson，见 VoucherOrderServiceImpl）
     */
    public static final String LOCK_ORDER_KEY = "lock:order:";
}
