package com.hmdp.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简易 RAG 知识库（对齐参考项目 Easy-RAG 思路）：
 * 启动时将 classpath:knowledge/ 下的 markdown 文档切分并向量化，
 * 存入内存向量库 {@link SimpleVectorStore}，供 {@link QuestionAnswerAdvisor} 检索。
 *
 * <p>知识文档内容来自 hmdp 业务库的店铺/优惠券说明，使 AI 能回答
 * "平台有哪些店铺分类""某个商圈有哪些店"等知识型问题。
 */
@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    /** 检索时返回的片段数量（对齐参考项目 maxResults=3） */
    private static final int TOP_K = 3;

    /** 检索相似度阈值（对齐参考项目 minScore=0.5） */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    @Bean
    public VectorStore vectorStore(org.springframework.ai.embedding.EmbeddingModel embeddingModel,
                                   @Value("${app.ai.rag.store-path:data/ai-vector-store.json}") String storePath,
                                   @Value("${app.ai.rag.rebuild:false}") boolean rebuild) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = new File(storePath);

        // 优先加载已持久化的向量快照，避免每次重启重复调用 embedding API（省配额、加快启动）
        if (!rebuild && storeFile.isFile()) {
            try {
                store.load(storeFile);
                log.info("已加载知识库向量快照：{}", storeFile.getAbsolutePath());
                return store;
            } catch (Exception e) {
                log.warn("知识库向量快照加载失败，将重新构建：{}", e.getMessage());
            }
        }

        List<Document> documents = loadKnowledgeDocuments();
        if (!documents.isEmpty()) {
            try {
                // 切分后向量化入库
                store.add(new TokenTextSplitter().split(documents));
                persistSnapshot(storeFile, store);
                log.info("知识库向量化完成并已持久化：{}", storeFile.getAbsolutePath());
            } catch (Exception e) {
                // 容错：未配置有效 API Key 时知识库向量化失败，应用仍可启动（仅无 RAG 增强）
                log.warn("知识库向量化失败，本次启动跳过 RAG 知识库（请检查 AI_API_KEY 配置）：{}", e.getMessage());
            }
        }
        return store;
    }

    /**
     * 将向量库保存到本地快照文件（知识库文档更新后，删除快照文件或将 rebuild 设为 true 即可重建）
     */
    private void persistSnapshot(File storeFile, SimpleVectorStore store) throws IOException {
        File parent = storeFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("创建向量快照目录失败：{}", parent);
        }
        store.save(storeFile);
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
