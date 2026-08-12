package com.hmdp.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * SimpleVectorStore 检索性能基准（Milvus 性价比评估用）： 用固定维度的伪随机向量模拟真实 embedding，测量不同数据规模下
 * 内存向量库的构建耗时、内存占用与相似度检索延迟。
 *
 * <p>
 * 不依赖外部 API（使用桩 EmbeddingModel），可在任意环境运行：
 * {@code mvn test -Dtest=SimpleVectorStoreBenchmarkTest}
 */
class SimpleVectorStoreBenchmarkTest {

    /**
     * 与 DashScope text-embedding-v4 一致的向量维度
     */
    private static final int DIM = 1024;

    /**
     * 评估规模梯度：当前真实规模（3 条）-> 千 -> 万 -> 十万
     */
    private static final int[] SCALES = {3, 1_000, 10_000, 100_000};

    private static final int WARMUP_ROUNDS = 3;

    private static final int MEASURE_ROUNDS = 20;

    @Test
    void benchmarkSimilaritySearchAcrossScales() {
        // 屏蔽 SimpleVectorStore 逐条 embedding 的 INFO 日志，避免淹没基准结果
        ((Logger) LoggerFactory.getLogger(SimpleVectorStore.class)).setLevel(Level.WARN);

        System.out.println("====== SimpleVectorStore 检索基准（维度=" + DIM + "，topK=3）======");
        System.out.printf("%-12s %-12s %-14s %-14s%n", "向量条数", "构建耗时(ms)", "内存占用(MB)", "平均检索延迟(ms)");
        for (int scale : SCALES) {
            benchmarkOneScale(scale);
        }
    }

    private void benchmarkOneScale(int n) {
        long heapBefore = usedHeapBytes();

        long t0 = System.nanoTime();
        SimpleVectorStore store = SimpleVectorStore.builder(new FixedDimEmbeddingModel(DIM)).build();
        List<Document> docs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            docs.add(Document.builder().text("knowledge-chunk-" + i).build());
        }
        store.add(docs);
        long buildMs = (System.nanoTime() - t0) / 1_000_000;
        long heapDeltaMb = (usedHeapBytes() - heapBefore) / 1024 / 1024;

        SearchRequest request = SearchRequest.builder()
                .query("附近有哪些美食店")
                .topK(3)
                .similarityThreshold(0)
                .build();

        // 预热，消除 JIT 编译影响
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            store.similaritySearch(request);
        }

        long totalNanos = 0;
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            long start = System.nanoTime();
            store.similaritySearch(request);
            totalNanos += System.nanoTime() - start;
        }
        double avgMs = totalNanos / 1_000_000.0 / MEASURE_ROUNDS;

        System.out.printf("%-12d %-12d %-14d %-14.3f%n", n, buildMs, heapDeltaMb, avgMs);

        // 释放引用帮助下一轮 GC
        store = null;
        docs = null;
        System.gc();
    }

    private static long usedHeapBytes() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 桩 EmbeddingModel：返回固定维度的伪随机向量，避免调用真实 embedding API
     */
    static class FixedDimEmbeddingModel implements EmbeddingModel {

        private final int dim;
        private final Random random = new Random(42);

        FixedDimEmbeddingModel(int dim) {
            this.dim = dim;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>(request.getInstructions().size());
            for (int i = 0; i < request.getInstructions().size(); i++) {
                embeddings.add(new Embedding(nextVector(), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return nextVector();
        }

        @Override
        public int dimensions() {
            return dim;
        }

        private float[] nextVector() {
            float[] vector = new float[dim];
            for (int d = 0; d < dim; d++) {
                vector[d] = random.nextFloat() * 2 - 1;
            }
            return vector;
        }
    }
}
