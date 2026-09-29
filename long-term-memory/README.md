# long-term-memory

> 对应文章：《我让 AI 记住"我是 Go 开发"，它回了我一句"好的"，然后什么都没发生》

## 这篇文章解决什么

`ChatMemory` 管的是**这一场对话说过什么**，窗口一滚就淘汰，重启也未必还在。

本模块做的是**另一层**：用户是谁、要 AI 怎么做——跨会话长期有效，跟会话无关。

| 层 | 存什么 | 生命周期 | 类比 |
|---|---|---|---|
| `MessageChatMemoryAdvisor` | 本场对话原文 | 会话级 | `HttpSession` |
| **本模块** | 用户身份与偏好 | 跨会话长期 | `Cookie` / Remember-Me |

持久化解决的是"跨重启"，不解决"跨会话"。这是两件事。

## 核心代码在哪

- `advisor.PreferenceAdvisor` —— 请求前查偏好，拼进 system prompt。**坑最集中在这一个类**
- `tool.PreferenceTools` —— `@Tool` 三个方法，让模型在对话里增删改记忆
- `repository.PreferenceRepository` —— JdbcTemplate 实现，生产换 Redis 只替换这一个类
- `common.UserContext` —— ThreadLocal 存 userId，防工具被诱导传参越权

## 环境要求

- JDK 21 / Spring Boot 4.1.0 / Spring AI 2.0.1
- Ollama：`ollama pull qwen2.5:14b`（需要 function calling 能力）
- H2 文件库内置，无需安装任何东西

按仓库规矩：`spring-boot-starter-jdbc` 和 `h2` **只在本子模块声明**，不进父 pom。

## 怎么跑

```bash
git clone https://github.com/jsf-deepwater/spring-ai-demo.git
cd long-term-memory
mvn spring-boot:run
```

启动后 `schema.sql` 自动建表（幂等，`IF NOT EXISTS`）。

塞两条初始记忆：

```bash
curl "http://localhost:8080/preference/add?userId=1&category=USER&preference=我是一名java开发者"
curl "http://localhost:8080/preference/add?userId=1&category=FEEDBACK&preference=我没说明白，你先问，不要瞎猜"

curl "http://localhost:8080/preference/list?userId=1"
```

## 你应该看到什么

```bash
curl "http://localhost:8080/chat?msg=我是谁&userId=1&conversationId=100"
```

偏好注入成功时，回答里会带出"java 开发者"这个身份。

**如果它答"您是提问的用户…"** → 说明偏好没查到，检查 `userId` 是不是传错了（文章坑三）。

**如果回答里冒出 `คณะกรรมฟUNCTION` 这类乱码** → 那是 qwen 的工具调用 token 没被解析，说明这次工具没真调（文章坑一）。

## 记忆分类

`category` 四类，抄自 Kimi 的记忆空间设计：

| 分类 | 含义 | 例子 |
|---|---|---|
| `USER` | 身份与偏好 | 我是 Java 开发者 |
| `FEEDBACK` | 对 AI 行为的纠正 | 我没说明白，你先问，不要瞎猜 |
| `PROJECT` | 项目背景 | 当前项目用 Spring Boot 4 |
| `REFERENCE` | 外部资料 | 内部文档在 xxx |

**分类不是花架子。** 实测中说一句"把关于我身份的记忆都删了"，模型只删了 `USER` 类，`FEEDBACK` 那条最值钱的指令活下来了。分类是误删的隔离带。

硬上限同样照抄 Kimi：**最多 50 条，每条 ≤ 500 字符**，写进了数据库 CHECK 约束和工具层校验。

## 接口

| 接口 | 说明 |
|---|---|
| `GET /chat?msg=&userId=&conversationId=` | 对话。偏好按 `userId` 查，历史按 `conversationId` 查 |
| `GET /preference/add?userId=&category=&preference=` | 显式新增 |
| `GET /preference/list?userId=` | 查看某用户全部记忆 |

## 数据源配置

```yaml
spring:
  datasource:
    url: jdbc:h2:file:${user.home}/.long-term-memory/ltm-db;DB_CLOSE_ON_EXIT=FALSE
```

> 用 `${user.home}` 而不是 `./data`：相对路径 `./` 跟的是 **JVM 工作目录**，从父工程启动时会生成到父工程根。用用户目录可以彻底避开，**连 `.gitignore` 都不用加**。

## 复现文章里的实验

> 每次改 `getOrder()` 都要**重启**才生效。

### 实验 1｜order 决定你拿到的是不是原话

```bash
curl "http://localhost:8080/chat?msg=你好&userId=1&conversationId=100"
curl "http://localhost:8080/chat?msg=今天天气怎么样&userId=1&conversationId=100"
curl "http://localhost:8080/chat?msg=讲讲虚拟线程&userId=1&conversationId=100"
```

在 `PreferenceAdvisor.before()` 里打印 `request.prompt().getInstructions()` 的条数，然后：

- `getOrder() = 0` → **4 条**，历史已混入
- `getOrder() = Ordered.HIGHEST_PRECEDENCE + 100` → **2 条**，干净

两个坑：

- **别用 `SimpleLoggerAdvisor` 观察**：它默认 order 是 0，排在 `MessageChatMemoryAdvisor`（`MIN+1000`）后面，永远只看到加工完的请求。
- **别用全新会话测**：没有历史可混，两次结果一样，你会误判 order 没用。

### 实验 2｜`augmentSystemMessage` 是覆盖不是追加

```java
System.out.println("old:" + request.prompt().getSystemMessage().getText());
Prompt newPrompt = request.prompt().augmentSystemMessage(" 每次说话前需要加上 Hello!");
System.out.println("new:" + newPrompt.getSystemMessage().getText());
```

```
old: 你是一名英语老师
new:  每次说话前需要加上 Hello!
```

正确写法是把原文本拼回来：`augmentSystemMessage(systemMessage.getText() + preferencePrompt)`。

### 实验 3｜userId / conversationId 混用

- 把 `userId` 传错 → 偏好查不到，AI 完全不认识你
- 把 `conversationId` 传错 → 偏好还在，但历史串味

### 实验 4｜工具写完，本轮不生效

```bash
curl "http://localhost:8080/chat?msg=记住我是Go开发者，然后告诉我我是什么开发者&userId=1&conversationId=200"
curl "http://localhost:8080/preference/list?userId=1"
curl "http://localhost:8080/chat?msg=我是什么开发者&userId=1&conversationId=201"
```

注入发生在请求前，工具调用发生在响应过程中，两者有时序差。

### 实验 5｜模型说"记住了"，库里没变

每句跑完**立刻**查 `/preference/list`，别攒到最后：

```bash
curl "http://localhost:8080/chat?msg=最近在看Python，你记一下&userId=1&conversationId=300"
curl "http://localhost:8080/chat?msg=我不是go的了&userId=1&conversationId=301"
curl "http://localhost:8080/chat?msg=算了，那个不用记了&userId=1&conversationId=302"
curl "http://localhost:8080/chat?msg=把关于我身份的记忆都删了&userId=1&conversationId=303"
```

**每步都查库对比，别信模型的回复。** 实测里"算了，那个不用记了"返回"好的"，数据库一条没删。

## 已知问题（都是特性，不是 bug）

1. **`qwen2.5:14b` 的 function calling 时灵时不灵。** 同一句话这次生效下次不生效，还可能吐出 `คณะกรรมฟUNCTION` 这类乱码——那是 qwen 的工具调用特殊 token 没被解析，当普通文本输出了。**这意味着工具写入路径没法写单元测试。**
2. **`UPDATE` 覆盖写导致旧事实永久消失。** `我是java开发者` 被改成 `我是Go开发者` 后，你再也查不出前者。生产应改成追加 + 生效标记。
3. **H2 文件库多实例不共享。** demo 够用，生产换 Redis / MySQL，只替换 `PreferenceRepository` 实现。
4. **没有缓存。** 偏好每轮同步查库，流式输出下会拖慢首 token。建议实验跑完再加 Caffeine——加了之后会分不清是时序问题还是缓存没失效。
5. **H2 的 `KeyHolder` 有个坑**：它会把带 `DEFAULT CURRENT_TIMESTAMP` 的列也一起返回，导致 `getKey()` 报"多个 key"。**注意此时插入其实已经成功了**，只是取 id 时炸的。改用 `SimpleJdbcInsert` 指定 `usingGeneratedKeyColumns("id")` 即可。

## 生产上还差什么

- [ ] 写入闸门：只有命中预设维度才写，否则返回 `no_update`
- [ ] 记忆表改成追加 + 生效标记，别直接 UPDATE
- [ ] 工具方法绝不接收 `userId` 参数，一律从 ThreadLocal 取（防跨账号越权）
- [ ] `UserContext.clear()` 必须在 `finally` 里（防线程池复用泄漏）
- [ ] 偏好查询加缓存 + 失效策略
- [ ] 多实例下换成 Redis / MySQL
- [ ] 写入回执校验：模型说"记住了"之后，回查一次库确认
