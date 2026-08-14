package com.hmdp.ai.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.hmdp.ai.rag.store.FileVectorSnapshotRepository;
import com.hmdp.ai.rag.store.RedisVectorSnapshotRepository;
import com.hmdp.ai.rag.store.VectorSnapshotRepository;

/**
 * 简易 RAG 知识库（对齐参考项目 Easy-RAG 思路）： 启动时将 classpath:knowledge/ 下的 markdown
 * 文档切分并向量化， 存入内存向量库 {@link SimpleVectorStore}，供 {@link QuestionAnswerAdvisor}
 * 检索。
 *
 * <p>
 * 知识文档内容来自 hmdp 业务库的店铺/优惠券说明，使 AI 能回答 "平台有哪些店铺分类""某个商圈有哪些店"等知识型问题。
 *
 * <p>
 * 向量快照的持久化介质由 {@code app.ai.rag.store} 决定（检索始终在内存完成，性能一致）：
 * <ul>
 * <li>{@code simple}（默认）：本地 JSON 文件（{@link FileVectorSnapshotRepository}），兼容现状；</li>
 * <li>{@code redis}：普通 Redis 字符串（{@link RedisVectorSnapshotRepository}），生产推荐——
 * 无需 RediSearch/Redis Stack，多实例共享、重启不丢、不依赖工作目录。</li>
 * </ul>
 */
@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    /**
     * 检索时返回的片段数量（对齐参考项目 maxResults=3）
     */
    private static final int TOP_K = 3;

    /**
     * 检索相似度阈值（对齐参考项目 minScore=0.5）
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    @Bean
    public VectorStore vectorStore(org.springframework.ai.embedding.EmbeddingModel embeddingModel,
            VectorSnapshotRepository snapshotRepository,
            @Value("${app.ai.rag.rebuild:false}") boolean rebuild) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        // 降级模式（无 AI_API_KEY，注入 FallbackEmbeddingModel，embed() 返回全 1 占位向量）：
        // 跳过快照加载/向量化/持久化，避免把占位向量写入快照，毒化后续真实启动的 RAG 检索
        if (embeddingModel instanceof FallbackEmbeddingModel) {
            log.info("降级模式（未配置 AI_API_KEY）：跳过 RAG 知识库向量化与快照读写");
            return store;
        }

        // 优先加载已持久化的向量快照，避免每次重启重复调用 embedding API（省配额、加快启动）
        if (!rebuild && snapshotRepository.load(store)) {
            return store;
        }

        List<Document> documents = loadKnowledgeDocuments();
        if (!documents.isEmpty()) {
            try {
                // 切分后向量化入库
                store.add(new TokenTextSplitter().split(documents));
                snapshotRepository.save(store);
                log.info("知识库向量化完成并已持久化");
            } catch (Exception e) {
                // 容错：未配置有效 API Key 时知识库向量化失败，应用仍可启动（仅无 RAG 增强）
                log.warn("知识库向量化失败，本次启动跳过 RAG 知识库（请检查 AI_API_KEY 配置）：{}", e.getMessage());
            }
        }
        return store;
    }

    /**
     * simple 模式（默认）：向量快照存本地 JSON 文件；路径支持绝对路径，
     * 生产 {@code java -jar} 启动建议配置绝对路径以规避工作目录差异
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.rag.store", havingValue = "simple", matchIfMissing = true)
    public VectorSnapshotRepository fileVectorSnapshotRepository(
            @Value("${app.ai.rag.store-path:data/ai-vector-store.json}") String storePath) {
        return new FileVectorSnapshotRepository(new File(storePath));
    }

    /**
     * redis 模式：向量快照存普通 Redis 字符串（无需 RediSearch/Redis Stack），
     * 解决文件快照依赖工作目录、容器重建丢失、多实例不一致问题
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.rag.store", havingValue = "redis")
    public VectorSnapshotRepository redisVectorSnapshotRepository(StringRedisTemplate stringRedisTemplate,
            @Value("${app.ai.rag.redis-key:ai:vector:snapshot}") String redisKey) {
        return new RedisVectorSnapshotRepository(stringRedisTemplate, redisKey);
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .build();
    }

    /**
     * 加载 classpath:knowledge/ 下的全部 .md 文档
     */
    private List<Document> loadKnowledgeDocuments() {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/*.md");
            for (Resource resource : resources) {
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                documents.add(new Document(
                        resource.getFilename(),
                        content,
                        Map.of("source", resource.getFilename())
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载知识库文档失败", e);
        }
        return documents;
    }
}
