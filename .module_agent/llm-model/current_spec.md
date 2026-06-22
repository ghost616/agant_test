LLM 模型的 CRUD 管理、独立参数配置、连通性验证、统一模型调用接口
## ModelInvoker 接口与 DTO 类型体系


- 创建 8 个 DTO 类（Message、UsageInfo、ToolDefinition、ToolCall、ToolCallDelta、ChatRequest、ChatResponse、ChatChunk），统一放置于 `dto/model/` 包，使用 Lombok @Data/@Builder/@NoArgsConstructor/@AllArgsConstructor
- 创建 `ModelInvoker` 接口（`service/model/`），定义 `invoke()` 同步调用、`invokeStream()` 流式调用（Flux）、`verify()` 连通性验证三个方法
- `ErrorCode.java` 新增 3 个 MODEL 模块错误码（MODEL-INVOKE-001、MODEL-VERIFY-001、MODEL-UNSUPPORTED-001）
- `pom.xml` 新增 `reactor-core` 依赖以支持 Flux 流式响应
- 前端新增 `types/model.ts`，定义对应的 TypeScript 接口（Message、ChatRequest、ChatResponse、ChatChunk、ToolCall、ToolCallDelta、ToolDefinition、UsageInfo）
- pom.xml 新增 spring-boot-starter-webflux 依赖，提供 WebClient 用于流式响应解析
- 新建 RestClientConfig 配置类（connectTimeout=10s, readTimeout=120s）
- 新建 OpenAIInvoker：invoke() POST /chat/completions（RestClient+Bearer）、invokeStream() SSE 解析（WebClient）、verify() GET /models
- 新建 AnthropicInvoker：消息格式双向转换（内部 Message ↔ content blocks 格式）、SSE 事件类型路由、x-api-key 认证
- 新建 AzureInvoker：继承 OpenAIInvoker，URL = /openai/deployments/{modelName}/chat/completions?api-version=...，api-key 认证
- 新建 OllamaInvoker：POST /api/chat，NDJSON 流式解析，maxTokens→num_predict，无认证
- 新建 DeepSeekInvoker/CustomInvoker：均继承 OpenAIInvoker，无额外逻辑
- 新建 ModelInvokerManager：@Component 工厂，根据 PlatformType 创建 Invoker
- ModelConfigController 新增 POST verify、POST chat、POST chat/stream（SSE）、GET invoker 四个端点
所有 Invoker 相关文件已从 `service/model/` 移至 `service/model/invoker/` 子包，包括：ModelInvoker.java（接口）、ModelInvokerManager.java（工厂）、6 个平台实现（OpenAIInvoker/AnthropicInvoker/AzureInvoker/OllamaInvoker/DeepSeekInvoker/CustomInvoker）。package 声明已同步更新为 `com.ghost616.platform.service.model.invoker`。ModelConfigController 的 import 引用已更新。

- ModelInvokerManager 新增 ConcurrentHashMap 缓存复用（cache.computeIfAbsent），提取 createInvoker 私有方法
- ModelInvokerManager 新增 register/evict/clear/cacheSize 方法支持外部注册、覆盖、删除缓存
- ModelInvokerManager 新增 getInvokerById(Long id) 方法，仅从缓存 Map 按 ID 查询，不触发创建，ID 不存在时返回 null
- PlatformType.DEEPSEEK 的 defaultBaseUrl 已修正为 "https://api.deepseek.com"（移除无效 /v1 路径段，官方 API 端点为 /chat/completions，无 /v1 路径）
- OpenAIInvoker.invokeStream() SSE 解析已修复：使用 concatMap + StringBuilder 跨 chunk 行缓冲机制替代 flatMap + split("\n")，避免 Netty TCP 分帧导致 JSON 行被截断后静默丢失
- OpenAIInvoker.parseStreamChunk 新增 error 字段检测：当 SSE 数据包含 {"error": {...}} 时，返回 finishReason="error" 的 chunk，错误信息写入 delta
- OpenAIInvoker.handleStreamError 可见性从 private 提升为 protected，供子类复用
- AzureInvoker.invokeStream() 已与 OpenAIInvoker 对齐：新增 AtomicBoolean hasContent 内容跟踪、条件性 concatWith stop chunk、onErrorResume(handleStreamError) 错误处理、doOnSubscribe 日志、行缓冲 SSE 解析

- OpenAIInvoker.invokeStream() 和 AzureInvoker.invokeStream() 的 SSE 行处理改用 concatMap 统一处理：检查行是否以 "data" 开头，跳过后的冒号和空白定位到 payload，[DONE] 返回 stop chunk，否则提取 JSON 调用 parseStreamChunk，其他行返回 Mono.empty()
- OpenAIInvoker.invokeStream() 和 AzureInvoker.invokeStream() 已移除行缓冲 concatMap（StringBuilder + indexOf("\n") 拆行），直接对 bodyToFlux 返回的原始块进行 data/[DONE] 检查，简化 SSE 处理管道
- OpenAIInvoker.invokeStream() 和 AzureInvoker.invokeStream() 已移除 concatWith 兜底 stop chunk 及关联的 AtomicBoolean hasContent 内容跟踪逻辑。`data: [DONE]` 已在 concatMap 中返回 `ChatChunk(finishReason="stop")`，无需额外兜底，避免流式输出末尾出现重复 stop chunk。
- OpenAIInvoker.invokeStream() 和 AzureInvoker.invokeStream() 的 concatMap 逻辑已改为直接处理纯 JSON 原始块：bodyToFlux 返回的原始块是纯 JSON（`{` 开头）→ 直接调用 parseStreamChunk 解析；`[DONE]` → 返回 stop chunk；其他 → Mono.empty()。移除了不再生效的 SSE `data:` 前缀检查逻辑。
- ChatChunk 新增 reasoning 字段，用于承载 DeepSeek thinking 模式的推理/思考内容
- OpenAIInvoker.parseStreamChunk 新增 reasoning_content 解析：从 delta 节点读取 reasoning_content 填充 ChatChunk.reasoning
- ChatChunk.java 已修复：替换损坏的内容，正确实现流式对话片段 DTO（delta/reasoning/toolCall/finishReason/index）
- OpenAIInvoker.buildMessages() 中 content 字段始终放入消息 Map（移除 null 判断），解决 API 报 "missing field `content`" 错误。此修复同时为继承 OpenAIInvoker 的 AzureInvoker 和 DeepSeekInvoker 生效。
- ChatChunk 的 toolCall（单数 ToolCallDelta）已改为 toolCalls（复数 List&lt;ToolCallDelta&gt;），支持一个流式 chunk 中携带多个工具调用。OpenAIInvoker.parseStreamChunk 遍历 tool_calls 数组收集所有 ToolCallDelta；AnthropicInvoker.handleStreamEvent 使用 List.of 包装单个元素；前端 model.ts ChatChunk 类型同步更新。ToolCallBufferHook/MessageSavePostHook/ChatService 中调用 .getToolCall() 的地方已适配为遍历 .getToolCalls() List。
- ModelInvoker 接口新增 toToolDefinition(ToolConfigDTO tool) 方法，将工具配置 DTO 转换为统一的 ToolDefinition
- OpenAIInvoker 实现 toToolDefinition()：解析 tool.getParameterSchema() JSON，若无 "type" 字段则用 LinkedHashMap 包装为 {"type":"object","properties":<parsed>}
- AnthropicInvoker/OllamaInvoker 同步实现 toToolDefinition()，逻辑与 OpenAIInvoker 一致
- AzureInvoker/DeepSeekInvoker/CustomInvoker 继承 OpenAIInvoker，无需修改
- OpenAIInvoker/AnthropicInvoker/OllamaInvoker/ModelInvoker 的 toToolDefinition() 方法均已添加 parameterSchema 的 null/blank 前置检查。若为空，直接返回仅含 name 和 description 的 ToolDefinition，避免 readTree(null)/readValue(null) 抛出 IllegalArgumentException 传播到上层。
## 模型配置 CRUD（ModelConfig 实体与 API）
- 创建 `ModelConfig` 实体类（`entity/`），继承 `BaseEntity`，字段：name、platformType（PlatformType 枚举）、apiKey、baseUrl、modelName、temperature、maxTokens、status（CommonStatus 枚举）、description
- 枚举增强：`CommonStatus` 和 `PlatformType` 新增 `code` 字段（@EnumValue），支持 MyBatis-Plus 枚举持久化
- `BaseEntity` 新增 `deleted` 字段（@TableLogic），配合全局逻辑删除配置
- 创建 DTO：`ModelConfigDTO`（响应）、`ModelCreateRequest`（新增，含 Jakarta Validation）、`ModelUpdateRequest`（编辑）
- 创建 `ModelConfigMapper`（`repository/`，extends BaseMapper<ModelConfig>）
- 创建 `ModelConfigService` 接口与实现（`service/model/`）：列表查询（支持 name/platformType/status 筛选 + 分页）、按ID查询、新增（名称去重）、编辑、逻辑删除、状态切换
- 创建 `ModelConfigController`（`controller/`）：
  - GET /api/models - 列表查询
  - GET /api/models/{id} - 详情
  - POST /api/models - 新增
  - PUT /api/models/{id} - 编辑
  - DELETE /api/models/{id} - 逻辑删除
  - PUT /api/models/{id}/status - 启用/禁用切换
- `application.yml` 新增 `type-enums-package` 扫描配置，移除冗余的 logic-delete 字段配置（改为注解驱动）
- `PageResult` 重构：移除 Spring Data Page 依赖，改用 MyBatis-Plus IPage
- `ModelConfigServiceImpl.create()` 方法中，当 baseUrl 为空或空白时，自动使用 `platformType.getDefaultBaseUrl()` 填充默认值
- `ModelConfigController` 新增 `GET /api/models/platform-config` 接口，返回所有平台的 platformType/defaultBaseUrl/modelNames 列表
- 新增 `PlatformConfigResponse` DTO（@Data @Builder，platformType/defaultBaseUrl/modelNames 字段）
- `ModelConfigService.list()` 方法已去掉分页，签名改为 `List<ModelConfigDTO> list(String name, PlatformType platformType, CommonStatus status)`，直接返回 `List<ModelConfigDTO>`
- `ModelConfigController.list()` 端点 `GET /api/models` 已去掉 `page`/`size` 参数，返回 `ApiResponse<List<ModelConfigDTO>>`
ModelConfigServiceImpl 的 update()、toggleStatus()、delete() 方法在数据库操作成功后调用 modelInvokerManager.evict(id) 清除旧的模型调用器缓存，确保下次调用时使用最新配置重新创建 Invoker。