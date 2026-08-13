package com.hmdp.ai.rag.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import com.hmdp.ai.rag.StubEmbeddingModel;

/**
 * 文件快照存储单元测试：验证保存 → 恢复 → 检索的完整链路（不依赖外部服务）
 */
class FileVectorSnapshotRepositoryTest {

    private final EmbeddingModel embeddingModel = new StubEmbeddingModel(16);

    private SimpleVectorStore newStoreWithDocs() {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(List.of(
                Document.builder().text("川味观是美食店").build(),
                Document.builder().text("西湖是景点").build()));
        return store;
    }

    @Test
    void save后load可完整还原向量库(@TempDir Path tempDir) throws IOException {
        Path snapshot = tempDir.resolve("sub").resolve("snapshot.json");
        FileVectorSnapshotRepository repo = new FileVectorSnapshotRepository(snapshot.toFile());

        repo.save(newStoreWithDocs());
        assertTrue(snapshot.toFile().isFile(), "快照文件应已写入（含自动创建目录）");

        // 模拟重启：新 store 实例从文件恢复
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
    void 无快照文件时load返回false(@TempDir Path tempDir) {
        FileVectorSnapshotRepository repo = new FileVectorSnapshotRepository(
                tempDir.resolve("missing.json").toFile());

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        assertFalse(repo.load(store));
    }
}
