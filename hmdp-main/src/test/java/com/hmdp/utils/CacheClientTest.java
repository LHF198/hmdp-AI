package com.hmdp.utils;

import com.hmdp.entity.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONUtil;

/**
 * CacheClient 单元测试：覆盖缓存穿透（空值缓存）、缓存击穿（互斥锁 / 逻辑过期）三种策略。
 * 通过 Mockito 隔离 Redis 依赖，不连接真实 Redis。
 */
class CacheClientTest {

    private static final String KEY = "cache:shop:1";
    private static final Long TTL = 30L;

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CacheClient cacheClient;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(stringRedisTemplate);
    }

    private static Shop shop(long id, String name) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(name);
        return shop;
    }

    // ==================== queryWithPassThrough：缓存穿透 ====================

    @Test
    void passThrough_shouldReturnCachedValueWithoutDbQuery() {
        Shop cached = shop(1L, "缓存店铺");
        when(valueOperations.get(KEY)).thenReturn(JSONUtil.toJsonStr(cached));
        Function<Long, Shop> dbFallback = mock(Function.class);

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertEquals("缓存店铺", result.getName());
        verify(dbFallback, never()).apply(any());
    }

    @Test
    void passThrough_shouldQueryDbAndWriteCacheOnMiss() {
        when(valueOperations.get(KEY)).thenReturn(null);
        Function<Long, Shop> dbFallback = id -> shop(id, "数据库店铺");

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertEquals("数据库店铺", result.getName());
        verify(valueOperations).set(eq(KEY), anyString(), eq(TTL), eq(TimeUnit.MINUTES));
    }

    @Test
    void passThrough_shouldCacheEmptyValueWhenDbReturnsNull() {
        // 缓存未命中且数据库无数据 → 写入 2 分钟空值缓存，防止穿透
        when(valueOperations.get(KEY)).thenReturn(null);
        Function<Long, Shop> dbFallback = id -> null;

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertNull(result);
        verify(valueOperations).set(eq(KEY), eq(""), eq(RedisConstants.CACHE_NULL_TTL), eq(TimeUnit.MINUTES));
    }

    @Test
    void passThrough_shouldReturnNullOnEmptyCacheValueWithoutDbQuery() {
        // 命中的是之前写入的空值缓存 → 直接返回 null，不再穿透数据库
        when(valueOperations.get(KEY)).thenReturn("");
        Function<Long, Shop> dbFallback = mock(Function.class);

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertNull(result);
        verify(dbFallback, never()).apply(any());
    }

    // ==================== queryWithMutex：互斥锁解决击穿 ====================

    @Test
    void mutex_shouldReturnCachedValueWhenHit() {
        Shop cached = shop(1L, "缓存店铺");
        when(valueOperations.get(KEY)).thenReturn(JSONUtil.toJsonStr(cached));
        Function<Long, Shop> dbFallback = mock(Function.class);

        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertEquals("缓存店铺", result.getName());
        verify(dbFallback, never()).apply(any());
        // 未获取锁，不应触发解锁脚本
        verify(stringRedisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    void mutex_shouldLockQueryAndWriteCacheOnMiss() {
        when(valueOperations.get(KEY)).thenReturn(null);
        // 加锁成功
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        Function<Long, Shop> dbFallback = id -> shop(id, "数据库店铺");

        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);

        assertEquals("数据库店铺", result.getName());
        verify(valueOperations).set(eq(KEY), anyString(), eq(TTL), eq(TimeUnit.MINUTES));
        // 释放锁（Lua 脚本原子比较删除）
        verify(stringRedisTemplate).execute(any(), eq(java.util.Collections.singletonList("lock:shop:1")), any());
    }

    @Test
    void mutex_shouldReleaseLockWhenDbQueryFails() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        Function<Long, Shop> dbFallback = id -> {
            throw new RuntimeException("数据库异常");
        };

        try {
            cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.MINUTES);
        } catch (RuntimeException ignored) {
            // 预期抛出
        }
        // 异常路径也必须释放锁，避免死锁
        verify(stringRedisTemplate).execute(any(), eq(java.util.Collections.singletonList("lock:shop:1")), any());
    }

    // ==================== queryWithLogicalExpire：逻辑过期解决击穿 ====================

    @Test
    void logicalExpire_shouldReturnDirectlyWhenNotExpired() {
        RedisData redisData = new RedisData();
        redisData.setData(shop(1L, "未过期店铺"));
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(10));
        when(valueOperations.get(KEY)).thenReturn(JSONUtil.toJsonStr(redisData));
        Function<Long, Shop> dbFallback = mock(Function.class);

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.SECONDS);

        assertEquals("未过期店铺", result.getName());
        verify(dbFallback, never()).apply(any());
    }

    @Test
    void logicalExpire_shouldReturnStaleDataAndRebuildAsyncWhenExpired() throws InterruptedException {
        RedisData redisData = new RedisData();
        redisData.setData(shop(1L, "过期旧数据"));
        redisData.setExpireTime(LocalDateTime.now().minusSeconds(1));
        when(valueOperations.get(KEY)).thenReturn(JSONUtil.toJsonStr(redisData));
        // 重建线程获取锁成功
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        Function<Long, Shop> dbFallback = mock(Function.class);
        when(dbFallback.apply(anyLong())).thenReturn(shop(1L, "重建新数据"));

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.SECONDS);

        // 过期时先返回旧数据保证可用性
        assertEquals("过期旧数据", result.getName());
        // 后台重建线程异步刷新缓存并释放锁（daemon 线程池，等待其执行完成）
        Thread.sleep(500);
        verify(dbFallback).apply(1L);
        verify(stringRedisTemplate).execute(any(), eq(java.util.Collections.singletonList("lock:shop:1")), any());
    }

    @Test
    void logicalExpire_shouldReturnNullWhenCacheMiss() {
        when(valueOperations.get(KEY)).thenReturn(null);
        Function<Long, Shop> dbFallback = mock(Function.class);

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, TTL, TimeUnit.SECONDS);

        assertNull(result);
        verify(dbFallback, never()).apply(any());
    }

    // ==================== setWithLogicalExpire：写入格式 ====================

    @Test
    void setWithLogicalExpire_shouldWrapDataWithExpireTime() {
        cacheClient.setWithLogicalExpire(KEY, shop(1L, "店铺"), 10L, TimeUnit.SECONDS);

        verify(valueOperations, times(1)).set(eq(KEY), anyString());
    }

    @Test
    void set_shouldWriteJsonWithTtl() {
        cacheClient.set(KEY, shop(1L, "店铺"), TTL, TimeUnit.MINUTES);

        verify(valueOperations).set(eq(KEY), anyString(), eq(TTL), eq(TimeUnit.MINUTES));
    }

    @Test
    void setWithLogicalExpire_writtenJson_shouldBeReadableBack() {
        // 验证写入格式与 queryWithLogicalExpire 的读取格式自洽（序列化 → 反序列化闭环）
        Shop shop = shop(1L, "闭环店铺");
        cacheClient.setWithLogicalExpire(KEY, shop, 10L, TimeUnit.SECONDS);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), captor.capture());
        RedisData parsed = JSONUtil.toBean(captor.getValue(), RedisData.class);
        // 数据内容可读回
        assertEquals("闭环店铺", JSONUtil.toBean(JSONUtil.parseObj(parsed.getData()), Shop.class).getName());
        // 逻辑过期时间为写入时刻 + 10s（允许秒级偏差）
        assertTrue(Math.abs(java.time.Duration.between(parsed.getExpireTime(), LocalDateTime.now().plusSeconds(10)).getSeconds()) <= 1);
    }
}
