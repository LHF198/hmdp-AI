# 黑马点评

基于 Spring Boot 的本地生活服务点评平台，涵盖用户系统、商铺管理、内容分享、优惠券秒杀等核心业务场景，并集成 AI 探店助手（智能问答 + RAG 知识库 + 业务工具调用）。
## 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot | REST API |
| 数据库 | MySQL 8.0 + MyBatis Plus 3.4.3 | 持久化存储 & ORM |
| 缓存 | Redis + Lettuce 6.1.6 + Redisson 3.13.6 | 多级缓存 / 分布式锁 / GEO / BitMap / Stream |
| AI | Spring AI 1.0.0 | 智能问答 / RAG / 会话记忆 / 工具调用 |
| 前端 | nginx + Vue2 + Element UI | 静态页面 + 反向代理（8080） |
| 工具库 | Hutool 5.7.17, Lombok | 通用工具 & 代码简化 |
| 脚本 | Lua | 秒杀原子操作 & 分布式锁释放 |

## 项目结构

```
src/main/java/com/hmdp/
├── HmDianPingApplication.java    # 启动类
├── ai/                            # AI 探店助手（Spring AI 整合模块）
│   ├── config/                    #   ChatClient / RAG 知识库配置
│   ├── controller/                #   问答接口（流式/非流式）
│   ├── memory/                    #   Redis 会话记忆
│   └── tool/                      #   店铺/优惠券查询工具
├── config/                        # Spring 配置（Redis、MVC、MyBatis）
├── controller/                    # 控制器层（8 个 Controller）
│   ├── BlogController.java        #   笔记相关接口
│   ├── BlogCommentsController.java
│   ├── FollowController.java      #   关注/取关
│   ├── ShopController.java        #   商铺查询/新增/更新
│   ├── ShopTypeController.java    #   商铺类型
│   ├── UploadController.java      #   图片上传
│   ├── UserController.java        #   用户登录/签到
│   ├── VoucherController.java     #   优惠券管理
│   └── VoucherOrderController.java #  秒杀下单
├── dto/                           # 数据传输对象
├── entity/                        # 实体类（User, Shop, Blog, Voucher, VoucherOrder...）
├── mapper/                        # MyBatis Mapper 接口
├── service/                       # 业务接口
│   └── impl/                      # 业务实现
└── utils/                         # 工具类（Redis ID 生成器、缓存工具、登录拦截器等）

src/main/resources/
├── application.yaml               # 主配置（数据库、Redis、端口、AI 模型）
├── db/hmdp.sql                    # 数据库建库建表 + 初始数据
├── knowledge/店铺信息.md           # RAG 知识库文档
├── prompts/system-prompt.st       # AI 系统提示词
├── mapper/VoucherMapper.xml       # MyBatis XML 映射
├── seckill.lua                    # 秒杀库存扣减 Lua 脚本
└── unlock.lua                     # 分布式锁释放 Lua 脚本
```

## 核心功能与 Redis 应用

### 用户模块
- 手机号验证码登录 / 注册（Session 管理）
- Token 刷新拦截器（双拦截器设计）
- 每日签到 + 签到统计（**Redis BitMap**）

### 商铺模块
- 商铺按类型查询（三种缓存方案对比：**逻辑过期 / 互斥锁 / 空值缓存**）
- 附近商铺搜索（**Redis GEO**，按坐标 + 距离排序）
- 商铺名称模糊搜索

### 笔记模块
- 发布笔记、查看详情、个人笔记列表
- 点赞 / 取消点赞（**Redis ZSet** 维护 Top-N 点赞列表）
- 关注流推送（**Redis Stream / List** 实现 Feed 流）
- 笔记热榜、滚动分页

### 优惠券模块
- 普通优惠券领取
- 秒杀优惠券抢购（**Redis 库存预减 + Lua 原子脚本**）
- 异步下单（线程池处理，**Redisson 分布式锁**防超卖）
- 全局唯一 ID 生成器（**Redis 自增**）

### 关注模块
- 关注 / 取关（**Redis Set** 存取关系）
- 共同关注查询（**Redis Set 交集**）

### 评论与消息模块
- 笔记评论的发布 / 分页查询 / 删除（仅作者）
- 消息中心：我的笔记收到的评论、关注我的人

### AI 探店助手
- 智能问答（OpenAI 协议兼容模型，支持流式 SSE 输出）
- 多轮会话记忆（**Redis 持久化**，TTL 可配，可切换进程内存）
- RAG 知识库增强（启动时将 `knowledge/*.md` 向量化入内存向量库）
- 业务工具调用（模型自动查询店铺 / 优惠券回答实时数据问题）

## API 概览

### 用户 `/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/code` | 发送短信验证码 |
| POST | `/user/login` | 验证码登录/注册 |
| POST | `/user/logout` | 退出登录 |
| GET | `/user/me` | 获取当前用户信息 |
| GET | `/user/info/{id}` | 查询用户详情 |
| GET | `/user/{id}` | 按 ID 查询用户 |
| POST | `/user/info` | 修改个人资料 |
| POST | `/user/sign` | 每日签到 |
| GET | `/user/sign/count` | 签到天数统计 |

### 商铺 `/shop`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/{id}` | 查询商铺详情（含缓存） |
| POST | `/shop` | 新增商铺 |
| PUT | `/shop` | 更新商铺（缓存同步） |
| GET | `/shop/of/type` | 按类型筛选（支持 GEO 坐标 + 城市） |
| GET | `/shop/of/name` | 按名称模糊搜索 |
| GET | `/shop/cities` | 店铺数据覆盖的城市列表 |
| GET | `/shop/map/list` | 地图模式全量店铺（含坐标） |

### 笔记 `/blog`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/blog` | 发布笔记 |
| GET | `/blog/{id}` | 查询笔记详情 |
| PUT | `/blog/like/{id}` | 点赞 / 取消点赞 |
| GET | `/blog/hot` | 热门笔记列表 |
| GET | `/blog/of/me` | 我的笔记 |
| GET | `/blog/of/user` | 指定用户的笔记 |
| GET | `/blog/of/follow` | 关注流（滚动分页） |
| GET | `/blog/likes/{id}` | 笔记点赞 Top-N 列表 |
| DELETE | `/blog/{id}` | 删除本人笔记 |

### 评论 `/blog-comments`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/blog-comments/{blogId}` | 分页查询笔记评论（含用户昵称/头像） |
| POST | `/blog-comments/{blogId}` | 发布评论 / 回复 |
| DELETE | `/blog-comments/{id}` | 删除本人评论 |

### 消息 `/message`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/message/comments` | 我的笔记收到的评论 |
| GET | `/message/follows` | 关注我的人 |

### 优惠券 `/voucher`、`/voucher-order`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/voucher` | 新增普通优惠券 |
| POST | `/voucher/seckill` | 新增秒杀优惠券 |
| GET | `/voucher/list/{shopId}` | 查看商铺优惠券 |
| POST | `/voucher-order/seckill/{id}` | 秒杀下单 |
| GET | `/voucher-order/list` | 我的秒杀订单列表 |

### AI 助手 `/api/ai`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 非流式问答（JSON） |
| POST | `/api/ai/chat/stream` | 流式问答（SSE） |
| GET | `/api/ai/chat/stream` | 流式问答（纯文本流，逐字渲染） |
| DELETE | `/api/ai/conversation/{id}` | 清空会话记忆 |
| GET | `/api/ai/health` | 健康检查 |

### 关注 `/follow`
| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/follow/{id}/{isFollow}` | 关注 / 取关 |
| GET | `/follow/or/not/{id}` | 是否已关注 |
| GET | `/follow/common/{id}` | 共同关注列表 |

### 文件 `/upload`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload/blog` | 上传笔记图片 |
| GET | `/upload/blog/delete` | 删除图片 |

## 缓存策略

项目中商铺查询实现了三种缓存方案，用于对比不同场景下的适用性：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|
| 空值缓存 | 实现简单，防止缓存穿透 | 额外内存占用 | 数据一致性要求不高 |
| 互斥锁 | 保证数据一致，避免缓存击穿 | 性能较差，可能死锁 | 数据一致性要求高 |
| 逻辑过期 | 性能最优 | 数据短期不一致 | 读多写少、高并发 |

## 快速开始

**环境要求**：JDK 17+ · MySQL 8.0+ · Redis 6.0+

**1. 初始化数据库**（脚本已包含自动建库）

```bash
mysql -u root -p < src/main/resources/db/hmdp.sql
```

**2. 修改配置**

编辑 `src/main/resources/application.yaml`，调整数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hmdp
    username: root
    password: your_password
  redis:
    host: 127.0.0.1
    port: 6379
```

**3. 启动项目**

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8081`。

若需启用 AI 探店助手，先设置环境变量（DashScope / OpenAI 兼容服务的 Key）：

```powershell
setx AI_API_KEY "sk-xxxx"
```

**4. 启动前端（可选）**

```powershell
cd frontend
.\nginx.exe -c conf\nginx.conf
```

浏览器访问 `http://localhost:8080`（nginx 将 `/api` 代理至后端 8081，`/api/ai` 保留前缀并关闭缓冲以支持流式输出）。

**5. 接口测试**

导入 `hmdp.postman_collection.json`（如有）到 Postman，或直接在浏览器访问：

```
http://localhost:8081/shop/1
```

## 许可证

MIT License