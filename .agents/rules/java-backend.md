---
description: Java 后端编码约定与架构约束，修改 src/main/java/com/hmdp 下任何文件时自动加载
globs: src/main/java/com/hmdp/**/*.java
alwaysApply: false
---

# Java 后端模块约定

## 技术栈

- Spring Boot 3.5.16 + JDK 17
- MyBatis-Plus 3.5.17（`mybatis-plus-spring-boot3-starter`）
- Redis（Lettuce 连接池） + Redisson 3.52.0（分布式锁）
- Spring AI 1.1.8（AI 模块，独立子包 `com.hmdp.ai`）
- Hutool 5.8.47（字符串用 `StrUtil`，日期用 `DateUtil`，Bean 拷贝用 `BeanUtil`）
- Lombok（`@Data` + `@EqualsAndHashCode(callSuper = false)` + `@Accessors(chain = true)`）

## 包结构与职责

| 包 | 职责 | 命名约定 |
|---|---|---|
| `controller` | REST 接口，仅做参数接收与 Result 返回 | `XxxController` |
| `service` | 业务接口定义 | `IXxxService extends IService<Xxx>` |
| `service.impl` | 业务实现 | `XxxServiceImpl` |
| `entity` | 数据库实体，对应 `tb_xxx` 表 | `Xxx` |
| `mapper` | MyBatis-Plus Mapper | `XxxMapper extends BaseMapper<Xxx>` |
| `dto` | 数据传输对象 + 统一响应体 | `XxxDTO` / `Result` |
| `config` | 配置类（`@Configuration`） | `XxxConfig` / `MvcConfig` |
| `utils` | 工具类（静态方法） | `XxxUtils` / `XxxHelper` |
| `ai` | AI 智能问答子模块（独立分层） | `com.hmdp.ai.*` |

## 强制约定

### 统一响应体
- 所有 Controller 方法返回 `Result`，通过 `Result.ok()` / `Result.fail()` 静态工厂创建
- **禁止**在 Controller 中直接返回 Map、String 或其他类型

### 依赖注入
- 使用 `@Resource`（Jakarta），**不用** `@Autowired`
- 字段注入（非构造器注入），与项目现有风格保持一致

### Entity 规范
```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_xxx")
public class Xxx implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // ...
}
```

### Service 规范
- 接口：`IXxxService extends IService<Xxx>`，方法返回 `Result`
- 实现：`XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements IXxxService`
- 业务逻辑在 Service 层，Controller 不做业务判断

### Controller 规范
- `@RestController` + `@RequestMapping("/xxx")`
- 方法用 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- Javadoc 注释说明接口用途和参数含义

### 缓存约定
- Redis key 命名：`业务:子业务:id`，如 `cache:shop:1`
- 缓存工具使用 `CacheClient`（封装了常见缓存模式：穿透/击穿/逻辑过期）
- Redis GEO 键格式：`shop:geo:{typeId}`

### 测试约定
- 框架：JUnit 5 + Mockito（纯单元测试，不启动 Spring 容器）
- 测试类命名：`XxxTest`，放在 `src/test/java` 对应包下
- Mock 注入使用 `ReflectionTestUtils.setField()`
- 测试方法命名：中文驼峰式，如 `seckillVoucher_库存不足时失败()`
- `UserHolder` 线程变量需在 `@BeforeEach` / `@AfterEach` 中设置/清理

### AI 模块（`com.hmdp.ai`）
- 独立子包，有自己的 config/controller/dto/memory/service/tool 分层
- 使用 Spring AI OpenAI Starter（兼容阿里云百炼 DashScope）
- AI 限流：每 IP 每分钟 30 次
- AI 记忆：Redis 存储，TTL 1 天
