# spring-ai-demo

公众号合集《SpringAI2.0》的**配套可运行代码仓库**：一篇文章 = 一个子模块，clone 下来就能跑。

不是教程，是每篇实战文的落地代码。文章讲思路和踩坑，这里放能直接执行的完整工程。

---

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | |
| Spring Boot | 4.1.0 | |
| Spring AI | 2.0.1 | ⚠️ 不是 2.0.0 —— 2.0.0 有个 bug，2.0.1 修掉了 |
| 模型 | Ollama（本地） | 默认只用 Ollama，不依赖任何云 API Key |

## 快速开始

```bash
# 1. 装 Ollama 并拉一个模型（以 qwen2.5:14b 为例，按模块 README 的要求换）
ollama pull qwen2.5:14b

# 2. 克隆 & 进入任意模块
git clone 【仓库地址】
cd spring-ai-demo/tool-permission

# 3. 跑
mvn spring-boot:run
```

每个模块都是独立的 Spring Boot 应用，**互不影响，不需要全量构建整个仓库**。

---

## 模块索引

| 子模块 | 对应文章 | 一句话主题 | 外部依赖 | 状态 |
|---|---|---|---|---|
| `tool-permission` | 《我们给系统上了 AI，第三天它自己删了一条数据》 | 用装饰器模式给 Tool Calling 加角色权限：运行时拦截 + fail-close | 仅 Ollama | ✅ |

> 新文章发布后，这个表会往下加行。想找某篇的代码，直接搜文章标题里的关键词。

---

## 仓库约定

1. **每个模块独立可跑**。默认 profile 只用 Ollama —— 不需要装数据库、不需要申请 API Key。
2. **重依赖走 Profile 隔离**。PGVector / Milvus 这类需要额外环境的，全部 `@Profile("rag")` 之类隔离，默认不启用，用得到才在模块 README 里说明。
3. **父 pom 只放 100% 通用的依赖**（web / ollama / test / lombok）。模型 starter、webflux、chat-memory 等按需下沉到子模块 —— 这样避免了多 `ChatModel` bean 冲突、web 与 webflux 栈冲突这类难排查的问题。
4. **每个子模块有 README**，写清对应哪篇文章、怎么跑、需要什么环境。
5. **仓库里的代码是"改完之后"的版本**，不是文章里演示错误演进的中间态。想抄就抄仓库的。

---

## 相关文章

持续更新于公众号，合集《SpringAI2.0》。
