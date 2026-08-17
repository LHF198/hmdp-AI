# HeiMa DianPing (Black Horse Review)

> [中文](README.md) | English

A local life services review platform built with Spring Boot, covering core business scenarios such as user system, shop management, content sharing, and voucher seckill, with an integrated AI Shop Assistant (intelligent Q&A + RAG knowledge base + business tool calling).

## Tech Stack

| Layer | Technology | Description |
|------|------|------|
| Backend framework | Spring Boot 3.5.16 (JDK 17) | REST API |
| Database | MySQL 5.7+/8.0 + MyBatis-Plus 3.5.17 | Persistence & ORM |
| Cache | Redis 7.2.15 + Lettuce + Redisson 3.52.0 | Multi-level cache / distributed locks / GEO / BitMap / Stream |
| AI | Spring AI 1.1.8 | Intelligent Q&A / RAG / chat memory / tool calling |
| Frontend | Vue 3.5 + Vite 7 + Element Plus 2.14 + Pinia 3 + Vue Router 5 | SPA (nginx static hosting + reverse proxy on 8080) |
| Utilities | Hutool 5.8.47, Lombok | Common utilities & code simplification |
| Scripts | Lua | Atomic seckill operations & distributed lock release |

## Project Structure

```
src/main/java/com/hmdp/
├── HmDianPingApplication.java    # Application entry
├── ai/                            # AI Shop Assistant (Spring AI integration module)
│   ├── config/                    #   ChatClient / RAG knowledge base config
│   ├── controller/                #   Q&A endpoints (streaming / non-streaming)
│   ├── dto/                       #   Request/response objects
│   ├── memory/                    #   Redis chat memory (atomic merge via Lua)
│   ├── service/                   #   Conversation orchestration (RAG + tool calling)
│   ├── tool/                      #   Shop / voucher query tools
│   └── web/                       #   IP rate-limit interceptor
├── config/                        # Spring config (Redis, MVC, MyBatis)
├── controller/                    # Controller layer (10 controllers)
│   ├── BlogController.java        #   Blog endpoints
│   ├── BlogCommentsController.java
│   ├── FollowController.java      #   Follow / unfollow
│   ├── MessageController.java     #   Message center (comment/follow notifications)
│   ├── ShopController.java        #   Shop query / create / update
│   ├── ShopTypeController.java    #   Shop types
│   ├── UploadController.java      #   Image upload
│   ├── UserController.java        #   User login / check-in
│   ├── VoucherController.java     #   Voucher management
│   └── VoucherOrderController.java #  Seckill orders
├── dto/                           # Data transfer objects
├── entity/                        # Entities (User, Shop, Blog, Voucher, VoucherOrder...)
├── mapper/                        # MyBatis mapper interfaces
├── service/                       # Service interfaces
│   └── impl/                      # Service implementations
└── utils/                         # Utilities (Redis ID worker, cache helper, login interceptor, etc.)

src/main/resources/
├── application.yaml               # Main config (port, AI models, MyBatis)
├── application-dev.yaml           # Dev profile (local MySQL/Redis, out-of-the-box)
├── application-prod.yaml          # Prod profile (everything injected via env vars)
├── db/hmdp.sql                    # Schema + seed data
├── db/migration_v2_db_optimize.sql # DB optimization migration script
├── knowledge/店铺信息.md           # RAG knowledge base documents
├── prompts/system-prompt.st       # AI system prompt
├── mapper/VoucherMapper.xml       # MyBatis XML mapping
├── seckill.lua                    # Atomic seckill validation/deduction script
└── unlock.lua                     # Distributed lock release script
```

## Core Features & Redis Usage

### User Module
- Phone + SMS-code login / registration (Session management)
- Token refresh interceptor (dual-interceptor design)
- Daily check-in + check-in statistics (**Redis BitMap**)

### Shop Module
- Shop lookup by type (three cache strategies compared: **logical expiry / mutex / null-value cache**)
- Nearby shop search (**Redis GEO**, sorted by coordinates + distance)
- Shop name fuzzy search

### Blog Module
- Publish blogs, view details, personal blog list
- Like / unlike (**Redis ZSet** maintains Top-N like lists)
- Follow feed push (**Redis Stream / List** feed)
- Blog hot list, scroll pagination

### Voucher Module
- Normal voucher claiming
- Seckill voucher purchase (**Redis stock pre-deduction + atomic Lua script**)
- Async order placement (thread pool, **Redisson distributed locks** prevent overselling)
- Global unique ID worker (**Redis INCR**)

### Follow Module
- Follow / unfollow (**Redis Set**)
- Mutual follow lookup (**Redis Set intersection**)

### Comments & Messages Module
- Blog comment create / paginated query / delete (author only)
- Message center: comments on my blogs, my followers

### AI Shop Assistant
- Intelligent Q&A (OpenAI-protocol-compatible models, SSE streaming supported)
- Multi-turn chat memory (**Redis-persisted**, configurable TTL, switchable to in-memory)
- RAG knowledge base augmentation (`knowledge/*.md` vectorized into an in-memory vector store at startup)
- Business tool calling (the model queries shops / vouchers on demand to answer real-time questions)

## API Overview

### User `/user`
| Method | Path | Description |
|------|------|------|
| POST | `/user/code` | Send SMS verification code |
| POST | `/user/login` | Login / register with code |
| POST | `/user/logout` | Logout |
| GET | `/user/me` | Get current user info |
| GET | `/user/info/{id}` | Get user profile |
| GET | `/user/{id}` | Get user by ID |
| POST | `/user/info` | Update profile |
| POST | `/user/sign` | Daily check-in |
| GET | `/user/sign/count` | Check-in statistics |

### Shop `/shop`
| Method | Path | Description |
|------|------|------|
| GET | `/shop/{id}` | Get shop details (cached) |
| POST | `/shop` | Create shop |
| PUT | `/shop` | Update shop (cache sync) |
| GET | `/shop/of/type` | Filter by type (GEO coords + city supported) |
| GET | `/shop/of/name` | Fuzzy search by name |
| GET | `/shop/cities` | Cities covered by shop data |
| GET | `/shop/map/list` | All shops with coordinates (map mode) |

### Blog `/blog`
| Method | Path | Description |
|------|------|------|
| POST | `/blog` | Publish blog |
| GET | `/blog/{id}` | Get blog details |
| PUT | `/blog/like/{id}` | Like / unlike |
| GET | `/blog/hot` | Hot blog list |
| GET | `/blog/of/me` | My blogs |
| GET | `/blog/of/user` | Blogs of a given user |
| GET | `/blog/of/follow` | Follow feed (scroll pagination) |
| GET | `/blog/likes/{id}` | Top-N like list of a blog |
| DELETE | `/blog/{id}` | Delete own blog |

### Comments `/blog-comments`
| Method | Path | Description |
|------|------|------|
| GET | `/blog-comments/{blogId}` | Paginated blog comments (with user nickname/avatar) |
| POST | `/blog-comments/{blogId}` | Post comment / reply |
| DELETE | `/blog-comments/{id}` | Delete own comment |

### Messages `/message`
| Method | Path | Description |
|------|------|------|
| GET | `/message/comments` | Comments received on my blogs |
| GET | `/message/follows` | Users who follow me |

### Vouchers `/voucher`, `/voucher-order`
| Method | Path | Description |
|------|------|------|
| POST | `/voucher` | Create normal voucher |
| POST | `/voucher/seckill` | Create seckill voucher |
| GET | `/voucher/list/{shopId}` | List vouchers of a shop |
| POST | `/voucher-order/seckill/{id}` | Place seckill order |
| GET | `/voucher-order/list` | My seckill orders |

### AI Assistant `/api/ai`
| Method | Path | Description |
|------|------|------|
| POST | `/api/ai/chat` | Non-streaming Q&A (JSON) |
| POST | `/api/ai/chat/stream` | Streaming Q&A (SSE) |
| GET | `/api/ai/chat/stream` | Streaming Q&A (plain-text stream, character-by-character rendering) |
| DELETE | `/api/ai/conversation/{id}` | Clear conversation memory |
| GET | `/api/ai/health` | Health check |

### Follow `/follow`
| Method | Path | Description |
|------|------|------|
| PUT | `/follow/{id}/{isFollow}` | Follow / unfollow |
| GET | `/follow/or/not/{id}` | Check if followed |
| GET | `/follow/common/{id}` | Mutual follow list |

### Upload `/upload`
| Method | Path | Description |
|------|------|------|
| POST | `/upload/blog` | Upload blog image |
| GET | `/upload/blog/delete` | Delete image |

## Caching Strategies

The shop query module implements three cache strategies to compare their suitability across scenarios:

| Strategy | Pros | Cons | Best for |
|------|------|------|------|
| Null-value cache | Simple, prevents cache penetration | Extra memory usage | Low consistency requirements |
| Mutex lock | Consistent data, prevents cache breakdown | Lower performance, deadlock risk | High consistency requirements |
| Logical expiry | Best performance | Short-term data inconsistency | Read-heavy, high concurrency |

## Quick Start

**Requirements**: JDK 17+ · MySQL 5.7+ (8.0 recommended) · Redis 6.0+

**1. Initialize the database** (the script creates the schema automatically)

```bash
mysql -u root -p < src/main/resources/db/hmdp.sql
```

**2. Configure**

Edit `src/main/resources/application.yaml` and adjust the MySQL / Redis connection settings:

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

**3. Start the backend**

```bash
mvn spring-boot:run
```

The service runs at `http://localhost:8081` by default.

To enable the AI Shop Assistant, set the API key first (DashScope / any OpenAI-compatible service):

```powershell
setx AI_API_KEY "sk-xxxx"
```

If the variable is not set, the application still starts normally (the AI module falls back automatically and the Q&A endpoints return a "not configured" message; all other features keep working). After `setx`, restart your terminal/IDE for the environment variable to take effect.

**4. Start the frontend (optional)**

The frontend is a Vue 3 SPA (source in `frontend/src`); the build output goes to `frontend/html/dist/app`. nginx serves the SPA at the root path (legacy MPA pages remain in the dist root for easy rollback):

```powershell
cd frontend
npm install        # first run only
npm run build      # build the SPA to html/dist/app
.\nginx.exe -c conf\nginx.conf
```

Open `http://localhost:8080` in a browser (nginx proxies `/api` to the backend on 8081; `/api/ai` keeps its prefix and disables buffering to support streaming output).

**5. API testing**

Import `hmdp.postman_collection.json` (if present) into Postman, or simply open:

```
http://localhost:8081/shop/1
```

## License

MIT License

## Changelog

### 2026-08
- **L2 SPA Refactoring Complete**: Frontend migrated from legacy MPA to Vue 3 SPA (Vite build + nginx deployment), with legacy MPA pages preserved for rollback
- **AI Shop Assistant Enhanced**: Integrated Spring AI + RAG knowledge base + Redis chat memory, with streaming SSE output support
- **UI/UX Improvements**:
  - Homepage waterfall cards now adapt to original image aspect ratio (consistent with detail page)
  - Blog detail page title display + long title ellipsis
  - Shop detail page card centering fix
  - Global design system (tokens.css) + glassmorphism theme
- **Performance Optimization**: nginx gzip compression (main bundle -62%), on-demand icon registration (293 → 25)
- **Architecture Improvements**: CSS architecture refactoring (page-level style isolation), componentization (EmptyState/LikeIcon)
