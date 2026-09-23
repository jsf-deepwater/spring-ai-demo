# tool-permission

> 对应文章：《我们给系统上了 AI，第三天它自己删了一条数据》

## 这篇文章解决什么

Tool Calling 一旦开放，模型能调用哪些工具，就等同于"把哪些系统能力交给了模型"。

起因是实测某实时数据平台时，一句"把这个设备删掉"就删掉了设备、点位及其全部历史数据——而后台只提供"能不能用 agent"的总开关，提示词又是全局的、无法按人区分。

本文解决的就是：**怎么在代码层给工具加权限闸**。

另外文章里对比了两种方案，结论是**它们解决的是不同问题**：

- **运行时拦截**（本文采用）：有这个功能，但你这次不能做 —— 角色、数据范围、审批状态
- **注册阶段裁剪**：这个租户/版本有没有这个功能 —— 多租户、按套餐开放

## 核心代码在哪

| 类 / 注解 | 作用 |
|---|---|
| `@AuthRoles` | 打在 `@Tool` 方法上，`roles = {"ADMIN"}` 声明"哪些角色能调这个工具" |
| `PermissionToolCallWrapper` | 装饰器，包一层 `ToolCallback`。构造时用工具名反查 Method、读注解，执行前比对 `ToolContext` 里的 role |
| `PermissionTool` | 演示用工具集：`getDevice` ADMIN/USER 都能调，`deleteDevice` 仅 ADMIN |
| `agent(...)` @Bean | **关键点**：注册的是 `ToolCallbacks.from(new PermissionTool())` 再逐个包装后的数组，不是原始对象 |

> ⚠️ `.defaultTools(new PermissionTool())` 这种写法会让权限整体失效——Spring AI 内部转出来的 callback 是未包装的原始版本，且不报任何错。

## 环境要求

- JDK 21 / Spring Boot 4.1.0 / Spring AI 2.0.1
- Ollama：`ollama pull qwen2.5:14b`（见 `application.yml`）
- 无数据库、无云 API Key

## 怎么跑

```bash
git clone 【仓库地址】
cd spring-ai-demo/tool-permission
mvn spring-boot:run
```

```bash
# USER 想删设备 → 被拦截，模型转告"需要更高权限"
curl "http://localhost:8080/chat?role=USER&msg=把设备%20dev-001%20删掉"

# ADMIN 想删设备 → 放行执行
curl "http://localhost:8080/chat?role=ADMIN&msg=把设备%20dev-001%20删掉"

# USER 查询 → 放行
curl "http://localhost:8080/chat?role=USER&msg=查一下设备%20dev-001"
```

## 你应该看到什么

- `role=ADMIN` 请求删除 → 工具执行，控制台打出 `[工具执行] deleteDevice`
- `role=USER` 请求删除 → 控制台出现 `[权限拦截] 工具=deleteDevice role=USER`，模型转告用户无权限

⚠️ **关键验证**：被拦截时控制台**不应该**出现 `[工具执行] deleteDevice`。
如果出现了，说明拦截没生效——大概率是注册那步传了原始对象。
这个验证比看返回文案靠谱，因为"返回了无权限但工具其实执行了"在界面上看不出来。

## 设计取舍（都写在文章里了）

**为什么拦截用返回值，不抛异常？**

1. 无权限是**预期内的业务结果**，不是异常。抛了会让全局异常处理器把它当故障报上去，真正的线上故障被淹没在告警噪音里。
2. 抛了也没用——Spring AI 默认把 RuntimeException 的 message 转成文本发回模型（`spring.ai.tools.throw-exception-on-error` 默认 false），绕一圈回到原点。

**但启动期该抛就抛**：漏配注解、或工具名反查失败，直接 `IllegalStateException` 让应用起不来。绝不能静默降级成放行。

## 已知的坑

1. **框架不给你 Method**：`ToolCallback` 不暴露背后是哪个方法，只能拿工具名去 `getDeclaredMethods()` 里反查。匹配不上 → 静默降级为放行。已在构造期 fail-fast 堵死。
2. **`getDeclaredMethods()` 不含继承方法**，工具类有父类会匹配不上。
3. **fail-open**：`roles == null` 时默认放行是致命的，必须 fail-close。
4. **拦截文案别写太细**：它会进对话历史、进 prompt，最终可能被模型复述给用户。说"需要更高权限"，别说"ADMIN 可以"。

## 本文没写完的部分

- 角色应从 `SecurityContextHolder` 取，不要靠手动传参（demo 里为了演示直接传的）
- 审计：谁在什么时候让 AI 删了什么，必须留痕
- 危险操作二次确认：删除类不可逆操作应返回"待确认"中间态
- 异步场景下 `ToolContext` 是否会串
