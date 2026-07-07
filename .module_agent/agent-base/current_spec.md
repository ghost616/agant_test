# agent-base 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## SystemToolManager

系统工具管理器，非 Spring 组件。通过构造函数注入 SystemToolProvider 接口发现并注册系统工具，提供 getSystemTool(name) 按名称获取、getToolDefinitions() 构建带 _sys_ 前缀的 ToolDefinition 列表。
## ToolManager

工具调用管理器，负责工具的注册、展开与调用。expandMcpTools：在构建 McpExpandedToolDTO 时，若 description 包含远程工具原始名称（remoteName），使用 String.replace() 替换为带配置名前缀的新名称（prefixedName = configName_remoteName）。
expandMcpTools 已改为 public 权限，供 AgentContextManager 在技能加载时展开 MCP 工具。
## HookInvoker / SystemHook / SystemPostHook

从 platform-app 迁移而来。HookInvoker 为 HOOK 执行契约接口；SystemHook 扩展 HookInvoker 新增 getIndex() 执行顺序控制；SystemPostHook 为标记接口继承 SystemHook。
## MessageSavePostHook

从 platform-app 迁移而来。消息保存后置 HOOK，在 AFTER_MESSAGE_RECEIVE 阶段缓存流式块，收到 finishReason=stop 时拼装消息调用 sessionManager.save() 持久化，并通过 toolCallQueueManager.enqueue() 入队工具调用。已去掉 @Component/@RequiredArgsConstructor，改为显式构造函数注入 SessionManager/AgentContextManager/ToolCallQueueManager。
## ToolExecutionTracker

从 platform-app 迁移而来。非 Spring 组件（已去掉 @Component），保留 @Slf4j。通过 ConcurrentHashMap 维护会话级别的工具执行状态（setExecuting/setDone/setFailed）和执行结果记录，提供 clear/getCurrentExecution/getAndClearResults 方法。
## ModelConfigData

ModelConfigData record（com.ghost616.agentbase.dto.model.ModelConfigData），包含字段：Long id, String apiKey, String baseUrl, String modelName, Double temperature, Integer maxTokens, String platformType。
## ModelInvokerFactory

ModelInvokerFactory 接口（com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory），定义 createInvoker(ModelConfigData) 方法，返回 ModelInvoker。用于解耦 ModelInvokerManager 与具体 invoker 创建逻辑。
## ModelInvokerManager

从 platform-app 迁移而来。已去掉 @Component/@RequiredArgsConstructor 及 RestClient.Builder/WebClient.Builder 字段，改为构造函数注入 ModelInvokerFactory。createInvoker 委托给 factory.createInvoker(config)。getInvoker 参数改为 ModelConfigData，通过 config.id() 缓存。提供 register/evict/clear/cacheSize/getInvokerById 方法。
## ChatService

ChatService 聊天服务，非 Spring 组件。通过构造函数注入 AgentContextManager/SessionManager/ModelInvokerManager/ObjectMapper/SystemToolManager/ChatDataProvider。提供 initHooks() 通过 ChatDataProvider 发现并注册 HOOK（区分 SystemHook/SystemPostHook），chat(ChatRequest) 方法构建消息上下文与工具列表，调用 ModelInvoker 流式推理，通过 HOOK 机制在 SESSION_START/BEFORE_MESSAGE_SEND/AFTER_MESSAGE_RECEIVE 阶段拦截处理。内部包含 parseLoadedSkills 解析已加载技能、foldMessageGroups 按 recentMessageCount 折叠历史消息。

## ChatDataProvider

聊天数据提供者接口（com.ghost616.agentbase.service.agent.ChatDataProvider），定义三个方法：getModelConfig(Long modelId) 按 ID 获取 ModelConfigData、updateSessionModelId(Long sessionId, Long modelId) 更新会话的模型 ID、getHooks() 获取所有已注册的 HookInvoker。用于解耦 ChatService 与具体数据访问层。

## ChatRequest

聊天请求 DTO（com.ghost616.agentbase.dto.chat.ChatRequest），从 platform-app 迁移而来，改包名为 com.ghost616.agentbase.dto.chat。包含字段：sessionId（必填）、content（必填）、modelId（可选）、thinking（可选）。
## AgentContextManager

AgentContextManager（非 @Component，通过 @Bean 注册）：注入 ContextDataProvider/SessionManager/ToolManager，管理会话上下文缓存 ConcurrentHashMap；提供 build(sessionId) 实例方法返回 Builder 内部类（支持 modelIdOverride 链式调用），Builder.build() 通过 cache.computeIfAbsent 使用 dataProvider 查询 agent/session 数据、toolManager 加载工具、sessionManager 获取历史消息，并在加载 skills 后遍历每条 SkillConfigDTO 的 skillTools，对 MCP_HTTP 类型工具调用 toolManager.expandMcpTools() 展开为 McpExpandedToolDTO 列表替换原始 DTO；保留 get/remove/addHistoryEntry 方法。
## ToolExecutionService

工具执行服务，非 Spring 组件。通过构造函数注入 ToolCallQueueManager/ToolManager/SystemToolManager/SessionManager/ChatService/AgentContextManager/ToolExecutionTracker/ObjectMapper。提供三个核心方法：
- executeTool(Long sessionId)：从队列获取下一个工具调用，解析调用器并异步执行，返回 ToolExecutionResult record(status/toolId/toolName/arguments/hasMore/message)
- getToolStatus(Long sessionId)：查询当前工具执行状态，返回 ToolStatusResult record(status/toolId/toolName/arguments/hasMore/result)
- continueAfterTools(Long sessionId)：检查无工具在执行后，持久化工具结果、添加历史记录、清理队列和跟踪器，构造 TOOL_CONTINUE_MARKER 请求并调用 chatService.chat() 返回 Flux
## JsonMapper

公用 JSON 工具类（com.ghost616.agentbase.util.JsonMapper），final 类私有构造器，提供 public static final ObjectMapper MAPPER 实例。供 ChatService/ToolExecutionService 等组件直接引用，替代构造器注入方式。
