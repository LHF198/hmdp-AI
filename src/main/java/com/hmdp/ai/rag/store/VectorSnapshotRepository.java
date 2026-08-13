package com.hmdp.ai.rag.store;

import java.io.IOException;

import org.springframework.ai.vectorstore.SimpleVectorStore;

/**
 * 知识库向量快照存取抽象：负责 {@link SimpleVectorStore} 的持久化与恢复。
 *
 * <p>
 * 快照内容为 SimpleVectorStore 的标准 JSON 序列化（文档 + 向量），与存储介质无关。
 * 两种实现：
 * <ul>
 * <li>{@link FileVectorSnapshotRepository}：本地 JSON 文件（simple 模式，默认，兼容现状）；</li>
 * <li>{@link RedisVectorSnapshotRepository}：普通 Redis 字符串（redis 模式，生产推荐）——
 * 仅存快照字符串，不需要 RediSearch/Redis Stack，检索仍在内存完成，性能与 simple 模式一致。</li>
 * </ul>
 *
 * <p>
 * 选型依据（详见 AI 模块升级分析）：当前知识库仅 3 条向量，实测 10 万条内存暴力检索
 * 也只需 252ms，远低于 LLM 首字延迟，因此 Milvus/向量索引对本项目均为成本倒挂；
 * Redis 作为生产必配组件，恰好能消除文件态快照的三个痛点（工作目录不可控、容器重建丢失、
 * 多实例不一致），是最低成本的持久化方案。
 */
public interface VectorSnapshotRepository {

    /**
     * 从快照恢复向量库。
     *
     * @return true 表示快照存在且加载成功；无快照或加载失败返回 false（调用方应重新构建）
     */
    boolean load(SimpleVectorStore store);

    /**
     * 将向量库持久化为快照。
     */
    void save(SimpleVectorStore store) throws IOException;
}
