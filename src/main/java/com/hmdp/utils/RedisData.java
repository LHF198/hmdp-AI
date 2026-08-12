package com.hmdp.utils;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 逻辑过期缓存载体：expireTime 为逻辑过期时间，过期后由后台线程异步重建数据
 */
@Data
public class RedisData {

    private LocalDateTime expireTime;
    private Object data;
}
