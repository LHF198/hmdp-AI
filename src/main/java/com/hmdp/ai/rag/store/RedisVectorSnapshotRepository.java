package com.hmdp.ai.rag.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 型快照存储：向量库 JSON 序列化存普通 Redis 字符串（redis 模式，生产推荐）。
 *
 * <p>
 * 设计要点（性价比选型结论的落地）：
 * <ul>
 * <li>只存快照字符串，<b>不建向量索引</b>——检索始终在内存 SimpleVectorStore 完成，
 * 因此普通 Redis 即可，无需 RediSearch/Redis Stack，也无需引入任何新依赖；</li>
 * <li>解决文件态快照三痛点：不依赖进程工作目录、容器重建/发版不丢、多实例共享一份快照；</li>
 * <li>Redis 异常（连接失败/快照损坏）时 load 返回 false，调用方降级为重新向量化并重试写入，
 * 与 AI 模块既有容错策略一致，不影响应用启动。</li>
 * </ul>
 */
public class RedisVectorSnapshotRepository implements VectorSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisVectorSnapshotRepository.class);

    private final StringRedisTemplate redis;

    private final String key;

    public RedisVectorSnapshotRepository(StringRedisTemplate redis, String key) {
        this.redis = redis;
        this.key = key;
    }

    @Override
    public boolean load(SimpleVectorStore store) {
        String json;
        try {
            json = redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取 Redis 向量快照失败（连接异常），将重新构建知识库：{}", e.getMessage());
            return false;
        }
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            // SimpleVectorStore 仅提供 load(File)/load(Resource)，用字节流包装避免落临时文件
            store.load(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8), "redis:" + key));
            log.info("已从 Redis 加载知识库向量快照：{}（{} 字符）", key, json.length());
            return true;
        } catch (Exception e) {
            log.warn("Redis 向量快照损坏，将重新构建：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void save(SimpleVectorStore store) throws IOException {
        // SimpleVectorStore 仅提供 save(File)，经临时文件中转后写入 Redis（每次启动仅一次，成本可忽略）
        Path tmp = Files.createTempFile("hmdp-vector-snapshot-", ".json");
        try {
            store.save(tmp.toFile());
            String json = Files.readString(tmp, StandardCharsets.UTF_8);
            redis.opsForValue().set(key, json);
            log.info("知识库向量快照已写入 Redis：{}（{} 字符）", key, json.length());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
