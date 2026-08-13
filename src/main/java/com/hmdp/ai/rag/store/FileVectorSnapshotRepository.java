package com.hmdp.ai.rag.store;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SimpleVectorStore;

/**
 * 文件型快照存储：向量库 JSON 序列化到本地文件（simple 模式）。
 *
 * <p>
 * 快照路径支持相对/绝对路径；生产环境用 {@code java -jar} 启动时建议配置绝对路径，
 * 避免启动目录不可控导致快照读不到/写不进、每次重启重复调用 embedding API。
 */
public class FileVectorSnapshotRepository implements VectorSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(FileVectorSnapshotRepository.class);

    private final File storeFile;

    public FileVectorSnapshotRepository(File storeFile) {
        this.storeFile = storeFile;
    }

    @Override
    public boolean load(SimpleVectorStore store) {
        if (!storeFile.isFile()) {
            return false;
        }
        try {
            store.load(storeFile);
            log.info("已加载知识库向量快照：{}", storeFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.warn("知识库向量快照加载失败，将重新构建：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void save(SimpleVectorStore store) throws IOException {
        File parent = storeFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("创建向量快照目录失败：{}", parent);
        }
        store.save(storeFile);
        log.info("知识库向量快照已保存：{}", storeFile.getAbsolutePath());
    }
}
