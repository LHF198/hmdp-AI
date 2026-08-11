package com.hmdp.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisIdWorker 单元测试：验证 ID 的「时间戳高位 + 序列号低位」拼接结构与自增键格式。
 * 通过 Mockito 隔离 Redis 依赖，不连接真实 Redis。
 */
class RedisIdWorkerTest {

    /** 与 RedisIdWorker.BEGIN_TIMESTAMP 一致：2022-01-01 00:00:00 UTC */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisIdWorker redisIdWorker;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisIdWorker = new RedisIdWorker(stringRedisTemplate);
    }

    @Test
    void nextId_shouldIncrementDailyKey() {
        // 模拟 Redis 自增返回 10
        when(valueOperations.increment(anyString())).thenReturn(10L);

        redisIdWorker.nextId("order");

        // 自增键按天划分：icr:{prefix}:yyyy:MM:dd
        String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        verify(valueOperations).increment("icr:order:" + expectedDate);
    }

    @Test
    void nextId_shouldAssembleTimestampAndSequence() {
        when(valueOperations.increment(anyString())).thenReturn(7L);

        long id = redisIdWorker.nextId("order");

        // 低位 32 位为序列号
        assertEquals(7L, id & 0xFFFFFFFFL);
        // 高位为「当前秒 - 起始秒」，允许执行期间跨秒产生 ±1 偏差
        long expectedTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        assertTrue(Math.abs((id >>> 32) - expectedTimestamp) <= 1,
                "时间戳高位偏差过大: id=" + id + ", expected=" + expectedTimestamp);
    }

    @Test
    void nextId_shouldIncreaseWithSequence() {
        // 序列号从 0 连续递增
        when(valueOperations.increment(anyString()))
                .thenReturn(0L, 1L, 2L);

        long id0 = redisIdWorker.nextId("order");
        long id1 = redisIdWorker.nextId("order");
        long id2 = redisIdWorker.nextId("order");

        // 同秒内序列号递增；不同秒时高位时间戳会不同，因此仅断言序列号部分严格递增
        assertEquals(0L, id0 & 0xFFFFFFFFL);
        assertEquals(1L, id1 & 0xFFFFFFFFL);
        assertEquals(2L, id2 & 0xFFFFFFFFL);
        // ID 整体保持单调不减（同秒递增；跨秒时间戳跳变也只会增大）
        assertTrue(id1 >= id0);
        assertTrue(id2 >= id1);
    }
}
