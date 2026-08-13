package com.hmdp.ai.rag;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * 测试共享桩 EmbeddingModel：对任意文本返回相同的归一化固定向量（任意文本间余弦相似度恒为 1），
 * 使快照存取测试聚焦"保存 → 恢复 → 检索"的存储链路，而非检索质量（检索质量由
 * {@code SimpleVectorStoreBenchmarkTest} 单独度量），不依赖外部 embedding API。
 */
public class StubEmbeddingModel implements EmbeddingModel {

    private final float[] vector;

    public StubEmbeddingModel(int dim) {
        vector = new float[dim];
        float norm = (float) Math.sqrt(dim);
        for (int d = 0; d < dim; d++) {
            vector[d] = 1f / norm;
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>(request.getInstructions().size());
        for (int i = 0; i < request.getInstructions().size(); i++) {
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vector;
    }

    @Override
    public int dimensions() {
        return vector.length;
    }
}
