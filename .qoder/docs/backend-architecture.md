# 后端架构详情

> 本文件从 AGENTS.md 拆分，包含后端核心子系统的实现要点。编码规范详见 `.qoder/rules/java-backend.md`。

## 核心子系统

### 缓存策略（CacheClient）

`CacheClient` 工具类封装三种缓存模式，业务层直接调用：

```java
@Resource
private CacheClient cacheClient;

// 1. 缓存穿透（逻辑 null 防穿透）
Shop shop = cacheClient.queryWithPassThrough(
    "cache:shop:" + id, Shop.class,
    () -> getById(id), 30L, TimeUnit.MINUTES
);

// 2. 缓存击穿（互斥锁防击穿）
Shop shop = cacheClient.queryWithMutex(
    "cache:shop:" + id, Shop.class,
    () -> getById(id), 30L, TimeUnit.MINUTES
);

// 3. 逻辑过期（异步刷新，不阻塞）
Shop shop = cacheClient.queryWithLogicalExpiration(
    "cache:shop:" + id, Shop.class,
    () -> getById(id), 30L, TimeUnit.MINUTES
);
```

- Redis key 命名规范：`业务:子业务:id`（如 `cache:shop:1`）
- Redis GEO 键格式：`shop:geo:{typeId}`

### 秒杀方案

三层防护架构：

1. **前置校验**：Service 层判断库存/重复下单
2. **Lua 脚本原子操作**：`seckill.lua` 在 Redis 中原子校验库存 + 扣减 + 写入消息队列
3. **异步下单**：`VoucherOrderHandler` 消费 Redis Stream 消息，异步创建订单

```java
// Lua 脚本执行
Long result = redisTemplate.execute(
    SECKILL_SCRIPT,
    List.of(voucherId.toString(), userId.toString())
);
```

### 分布式锁

- 实现：Redisson（`RedissonClient`），支持可重入
- 锁释放：`unlock.lua` Lua 脚本保证原子性
- 使用模式：
  ```java
  RLock lock = redissonClient.getLock("lock:order:" + userId);
  try {
      if (lock.tryLock(0, 5, TimeUnit.SECONDS)) {
          // 业务逻辑
      }
  } finally {
      lock.unlock();
  }
  ```

### AI 模块（`com.hmdp.ai`）

独立子包，有自己的分层结构：

| 子包 | 职责 |
|---|---|
| `config/` | ChatClient / RAG 知识库配置 |
| `controller/` | 问答接口（流式 SSE / 非流式） |
| `dto/` | 请求/响应对象 |
| `memory/` | Redis 会话记忆（Lua 原子合并，TTL 1 天） |
| `rag/` | RAG 知识库（向量存储） |
| `service/` | 对话编排（RAG + 工具调用） |
| `tool/` | 店铺/优惠券查询工具（含 `tool/vo/`） |
| `web/` | IP 限流拦截器（每 IP 每分钟 30 次） |

- 使用 Spring AI OpenAI Starter（兼容阿里云百炼 DashScope）
- 环境变量 `AI_API_KEY` 未设置时降级启动，AI 接口返回提示
