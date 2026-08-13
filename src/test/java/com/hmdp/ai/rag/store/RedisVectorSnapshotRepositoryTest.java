package com.hmdp.ai.rag.store;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.ArgumentCaptor;

import com.hmdp.ai.rag.StubEmbeddingModel;

/**
 * Redis 快照存储单元测试：验证快照 JSON 的写入/读取与恢复链路（Mockito 模拟
 * StringRedisTemplate，不依赖真实 Redis）。
 */
class RedisVectorSnapshotRepositoryTest {

    private static final String KEY = "ai:vector:snapshot";

    private final EmbeddingModel embeddingModel = new StubEmbeddingModel(16);

    private RedisVectorSnapshotRepository newRepo(ValueOperations<String, String> valueOps) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return new RedisVectorSnapshotRepository(template, KEY);
    }

    private SimpleVectorStore newStoreWithDocs() {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(List.of(
                Document.builder().text("川味观是美食店").build(),
                Document.builder().text("西湖是景点").build()));
        return store;
    }

    @Test
    void load_redis无快照时返回false() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.get(KEY)).thenReturn(null);
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        assertFalse(newRepo(ops).load(store), "键不存在应视为无快照");
    }

    @Test
    void save后load可完整还原向量库() throws Exception {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        RedisVectorSnapshotRepository repo = newRepo(ops);

        repo.save(newStoreWithDocs());

        // 捕获写入 Redis 的快照 JSON，验证内容完整性
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq(KEY), captor.capture());
        String snapshot = captor.getValue();
        assertTrue(snapshot.contains("川味观是美食店"), "快照应包含文档文本");
        assertTrue(snapshot.contains("西湖是景点"));

        // 模拟重启：新 store 实例从 Redis 字符串恢复（ByteArrayResource 反序列化路径）
        when(ops.get(KEY)).thenReturn(snapshot);
        SimpleVectorStore restored = SimpleVectorStore.builder(embeddingModel).build();
        assertTrue(repo.load(restored));

        List<Document> hits = restored.similaritySearch(SearchRequest.builder()
                .query("川味观")
                .topK(3)
                .similarityThreshold(0)
                .build());
        assertEquals(2, hits.size(), "恢复后应能检索到全部文档");
    }

    @Test
    void load_redis快照损坏时返回false不抛异常() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.get(KEY)).thenReturn("{not-json");

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        assertFalse(newRepo(ops).load(store), "损坏快照应降级为重新构建而非抛异常");
    }

    @Test
    void load_redis连接异常时返回false() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.get(KEY)).thenThrow(new RuntimeException("connection refused"));

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        assertFalse(newRepo(ops).load(store), "Redis 不可用应降级为重新构建而非抛异常");
    }

    @Test
    void save_redis写入失败时向上抛出由调用方容错() throws Exception {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        org.mockito.Mockito.doThrow(new RuntimeException("connection refused"))
                .when(ops).set(anyString(), anyString());

        RedisVectorSnapshotRepository repo = newRepo(ops);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> repo.save(newStoreWithDocs()));
    }
}
