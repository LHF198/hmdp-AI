package com.hmdp.utils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用 Redis 缓存工具类，封装三种典型缓存问题的解决方案：
 * <ul>
 *   <li>缓存穿透：写入空值占位（短 TTL）；</li>
 *   <li>缓存击穿：互斥锁重建（自旋等待）或逻辑过期重建（异步线程池，不阻塞读）；</li>
 *   <li>缓存雪崩：由调用方按业务为不同 key 传入差异化 TTL，避免集中失效。</li>
 * </ul>
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存重建线程池。使用 daemon 线程：应用关闭时不会阻止 JVM 退出（与秒杀消费线程池保持一致），
     * 避免逻辑过期重建任务未完成时残留僵尸进程。
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r, "cache-rebuild-worker");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdown() {
        CACHE_REBUILD_EXECUTOR.shutdownNow();
    }

    // 互斥锁重试上限（50ms * 20 ≈ 1s），超过后降级为直接查询，避免无限自旋
    private static final int MAX_RETRY_TIMES = 20;
    // 释放锁的 Lua 脚本（比较持有者标识一致才删除，防止误删他人锁）
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息（前面已经把非空字符串命中了）避免重复查询：直接返回null，防止后续请求继续穿透到数据库层
            //提升性能：减少对无效数据的数据库访问，保护数据库压力
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }

    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.存在，直接返回
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        String lockValue = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (lockValue != null) {
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey, lockValue);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }

    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁（循环重试，避免递归导致栈溢出/无限自旋）
        String lockKey = LOCK_SHOP_KEY + id;
        String lockValue = null;
        int retry = 0;
        while (lockValue == null && retry < MAX_RETRY_TIMES) {
            lockValue = tryLock(lockKey);
            if (lockValue == null) {
                // 4.2.获取锁失败，休眠 50ms 后重试
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                retry++;
            }
        }
        R r = null;
        try {
            // 4.3.查询数据库（重试超时未拿到锁时降级为直接查询，保证可用性）
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } finally {
            // 7.释放锁（仅持有才释放）
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
        // 8.返回
        return r;
    }

    /**
     * 尝试获取分布式锁（基于Redis SETNX实现）
     *
     * @param key 锁的键名
     * @return 获取成功返回持有者标识（释放锁时校验用），失败返回 null
     */
    private String tryLock(String key) {
        // 每次加锁生成唯一标识，释放时校验，防止误删他人锁
        String value = UUID.randomUUID().toString(true);
        // 键不存在，设置成功，返回true，反之亦然
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, value, 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag) ? value : null;
    }

    /**
     * 释放分布式锁：仅当锁的持有者标识与本次加锁一致时才删除（Lua 原子比较删除）
     *
     * @param key 锁的键名
     * @param value 加锁时返回的持有者标识
     */
    private void unlock(String key, String value) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
    }
}
