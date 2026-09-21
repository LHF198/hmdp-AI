# hmdp pgvector 接入交接

## 目标

将当前基于 `SimpleVectorStore` 的 RAG 知识库升级为可选的 pgvector 存储，
保留现有开发和降级路径，并通过可复现测试验证检索功能。

本任务只处理向量存储接入，不迁移业务 MySQL，不引入 Milvus。
（Spring Boot / Spring AI 已另行升级至 4.1.1 / 2.0.1，本任务无需再处理版本升级）

## 当前技术基线

- Spring Boot 4.1.1
- JDK 17
- Spring AI 2.0.1
- 业务数据库：MySQL 8.4
- 缓存：Redis 8.x，开发环境使用 DB 1
- 当前向量存储：`SimpleVectorStore`
- 当前 embedding 模型：`text-embedding-v4`，维度 1024
- 当前检索配置：`topK=3`，`similarityThreshold=0.5`

## 已准备好的内容

- Docker Compose：[docker-compose.pgvector.yml](../docker-compose.pgvector.yml)
- 初始化脚本：[01-create-extension.sql](../docker/pgvector/init/01-create-extension.sql)
- 本地端口隔离：
  - 前端 Vite：8082
  - 后端：8083
  - pgvector：5432
- 开发环境 Redis：DB 1

## 当前环境状态

`pgvector/pgvector:pg17` 镜像已拉取，容器已经启动并通过健康检查，
`vector` 扩展版本为 `0.8.6`，监听 `localhost:5432`。

复现或重新启动环境：

```bash
docker compose -f docker-compose.pgvector.yml up -d
docker compose -f docker-compose.pgvector.yml ps
```

验证扩展：

```bash
docker compose -f docker-compose.pgvector.yml exec pgvector \
  psql -U hmdp -d hmdp_vector \
  -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
```

预期返回已安装的 `vector` 扩展版本。

## 接入要求

1. 增加可选向量存储模式，例如 `app.ai.rag.store=pgvector`。
2. 保留 `simple` 和 `redis` 模式，不能破坏现有启动路径。
3. MySQL 继续负责业务数据，pgvector 只保存知识片段和向量。
4. 不使用 `spring-ai-starter-vector-store-pgvector` 默认自动配置直接接管
   MySQL DataSource，必须使用独立 DataSource 或显式创建 `PGVectorStore`。
5. 无 `AI_API_KEY` 的降级模式不得初始化 pgvector 表、扩展或索引。
6. 向量维度固定为 1024，距离度量使用余弦距离。
7. 建议使用 HNSW 索引，并明确记录索引参数。
8. 文档入库使用稳定 ID 和内容 hash，支持幂等 upsert、文档删除和全量重建。
9. 每个 chunk 至少保存：
   - source
   - section
   - chunkIndex
   - contentHash
   - version
   - embeddingModel
10. embedding 模型或维度变化时必须触发全量重建，不能复用旧向量。

## 现有代码风险点

- `VectorSnapshotRepository` 当前直接依赖 `SimpleVectorStore`，接入
  pgvector 后需要条件化装配，不能强制要求快照仓库存在。
- 当前自动配置降级逻辑只排除了 OpenAI 自动配置，需要确认无 Key 时
  pgvector 自动配置不会启动。
- `RagConfig` 当前在启动时执行向量化，pgvector 接入后应避免每个实例
  重复全量写入，改为增量同步或显式重建。
- 现有业务店铺、优惠券数据已有 `ShopQueryTool` 实时查询，不应复制成
  大量向量文档。
- 检索链路需要保留失败回退，pgvector 不可用时不能影响主业务启动。

## 验收标准

- pgvector 容器健康，`vector` 扩展可查询。
- 应用默认模式仍使用原有 `SimpleVectorStore`。
- 显式切换 pgvector 后可以完成文档写入、检索和删除。
- 无 AI Key 时应用仍可启动，pgvector 不产生写入。
- 重复执行同一份文档不会产生重复 chunk。
- 删除或更新文档后，旧 chunk 不会继续命中。
- 单元测试和集成测试通过：
  - `mvn compile`
  - `mvn test`
  - pgvector 定向集成测试
- 输出检索延迟、Top-K 召回结果和重建耗时，不能填写推测数据。

## 推荐实施顺序

1. 验证现有 pgvector 容器、扩展和基础 SQL。
2. 抽象文档入库流程，确保 SimpleVectorStore 行为不回归。
3. 创建独立 PostgreSQL DataSource 和显式 `PGVectorStore` Bean。
4. 增加 pgvector 模式配置和条件装配。
5. 实现稳定 ID、内容 hash、upsert、删除和重建。
6. 补充集成测试和失败回退测试。
7. 使用真实或固定测试 embedding 运行检索评测，记录指标。

## 交接提示词

```text
你是资深 Java 后端和 Spring AI 工程师。请在
/Users/joe/Develop/hmdp-AI 中完成 pgvector 接入。

先阅读 docs/pgvector-handoff.md、AGENTS.md、pom.xml、
src/main/java/com/hmdp/ai/config/RagConfig.java、
src/main/java/com/hmdp/ai/config/AiConfig.java、
src/main/java/com/hmdp/ai/config/AiFallbackEnvironmentPostProcessor.java
以及现有 RAG 测试。

强制约束：
- 不升级 Spring Boot、JDK、Spring AI；
- 不迁移 MySQL 业务库；
- 不引入 Milvus；
- 保留 SimpleVectorStore、Redis 快照和无 AI Key 降级路径；
- pgvector 作为可选模式，不能改变默认启动行为；
- 不编造性能指标；
- 所有修改后执行 mvn compile、mvn test 和定向集成测试。

实现内容：
- 使用 pgvector/pgvector:pg17；
- 独立 PostgreSQL DataSource，避免自动配置误用 MySQL；
- 1024 维、余弦距离、HNSW 索引；
- 稳定 chunk ID、内容 hash、幂等 upsert、删除和重建；
- 失败回退和健康检查；
- 输出实际验证命令、结果、未完成项和风险。

完成后用中文汇报，禁止只给方案不执行。
```
