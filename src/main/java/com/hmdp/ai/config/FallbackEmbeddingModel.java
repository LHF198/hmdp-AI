package com.hmdp.ai.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.lang.NonNull;

/**
 * 降级 EmbeddingModel：AI_API_KEY 未配置时替代 OpenAI EmbeddingModel， 保证 RAG
 * 向量库（SimpleVectorStore）可正常装配。
 * <ul>
 * <li>每个输入返回固定维度零向量，避免 SimpleVectorStore 调用 {@link EmbeddingModel#dimensions()}
 * 时因空结果抛 NoSuchElementException；</li>
 * <li>知识库快照加载路径不触发向量化；快照缺失时的向量化由 RagConfig 容错并跳过知识库增强。</li>
 * </ul>
 */
public class FallbackEmbeddingModel implements EmbeddingModel {

    /**
     * 固定向量维度：对齐 text-embedding-v4
     */
    private static final int EMBEDDING_DIMENSION = 1024;

    /**
     * 固定向量：维度对齐 text-embedding-v4（1024）。 值取非零（全 1.0）而非零向量：SimpleVectorStore
     * 的余弦相似度计算要求范数非零， 否则检索快照中的真实向量时抛 "Vectors cannot have zero norm"； 全 1
     * 向量与真实向量余弦≈0，低于相似度阈值，RAG 检索结果为空（降级模式不注入知识库上下文）。
     */
    @NonNull
    private static final float[] FALLBACK_VECTOR = createFallbackVector();

    @NonNull
    private static float[] createFallbackVector() {
        float[] vector = new float[EMBEDDING_DIMENSION];
        Arrays.fill(vector, 1.0f);
        return vector;
    }

    @Override
    @NonNull
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> results = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            results.add(new Embedding(FALLBACK_VECTOR, i));
        }
        return new EmbeddingResponse(results);
    }

    @Override
    @NonNull
    public float[] embed(@NonNull Document document) {
        return FALLBACK_VECTOR;
    }
}
