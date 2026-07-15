# agent-base 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## SystemToolManager

系统工具管理器，非 Spring 组件。通过构造函数注入 SystemToolProvider 接口发现并注册系统工具，提供 getSystemTool(name) 按名称获取、getToolDefinitions() 构建带 _sys_ 前缀的 ToolDefinition 列表。
## ToolManager

getSessionTools 父会话 CHILD 副本修正：当 info.sessionAuth()==ALL 时，只基于原始未展开配置生成一份 CHILD 副本（ToolConfigDTO，非 McpExpandedToolDTO，invoker=null，mcpOriginalConfig 指向自身），而非对每个展开工具各生成一份。PARENT 副本保持对每个展开 McpExpandedToolDTO 生成一份不变。子会话逻辑不变。
## HookInvoker / SystemHook / SystemPostHook

从 platform-app 迁移而来。HookInvoker 为 HOOK 执行契约接口；SystemHook 扩展 HookInvoker 新增 getIndex() 执行顺序控制；SystemPostHook 为标记接口继承 SystemHook。
## MessageSavePostHook

从 platform-app 迁移而来。消息保存后置 HOOK，在 AFTER_MESSAGE_RECEIVE 阶段缓存流式块，收到 finishReason=stop 时拼装消息调用 sessionManager.save() 持久化，并通过 toolCallQueueManager.enqueue() 入队工具调用。已去掉 @Component/@RequiredArgsConstructor，改为显式构造函数注入 SessionManager/AgentContextManager/ToolCallQueueManager。
## ToolExecutionTracker

从 platform-app 迁移而来。非 Spring 组件（已去掉 @Component），保留 @Slf4j。通过 ConcurrentHashMap 维护会话级别的工具执行状态（setExecuting/setDone/setFailed）和执行结果记录，提供 clear/getCurrentExecution/getAndClearResults 方法。
ConcurrentHashMap key 从 Long sessionId 改为 String(sessionId_toolId) 支持每个工具独立跟踪；setDone/setFailed/getCurrentExecution 追加 toolId 参数；clear/getAndClearResults 通过按 sessionId 前缀清理保持 session 级别语义。
## ModelConfigData

ModelConfigData record（com.ghost616.agentbase.dto.model.ModelConfigData），包含字段：Long id, String apiKey, String baseUrl, String modelName, Double temperature, Integer maxTokens, String platformType。
## ModelInvokerFactory

ModelInvokerFactory 接口（com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory），定义 createInvoker(ModelConfigData) 方法，返回 ModelInvoker。用于解耦 ModelInvokerManager 与具体 invoker 创建逻辑。
## ModelInvokerManager

从 platform-app 迁移而来。已去掉 @Component/@RequiredArgsConstructor 及 RestClient.Builder/WebClient.Builder 字段，改为构造函数注入 ModelInvokerFactory。createInvoker 委托给 factory.createInvoker(config)。getInvoker 参数改为 ModelConfigData，通过 config.id() 缓存。提供 register/evict/clear/cacheSize/getInvokerById 方法。
## ChatService

ChatService 聊天服务，非 Spring 组件。通过构造函数注入 AgentContextManager/SessionManager/ModelInvokerManager/ObjectMapper/SystemToolManager/ChatDataProvider。提供 initHooks() 通过 ChatDataProvider 发现并注册 HOOK（区分 SystemHook/SystemPostHook），chat(ChatRequest) 方法构建消息上下文与工具列表，调用 ModelInvoker 流式推理，通过 HOOK 机制在 SESSION_START/BEFORE_MESSAGE_SEND/AFTER_MESSAGE_RECEIVE 阶段拦截处理。内部包含 parseLoadedSkills 解析已加载技能、foldMessageGroups 按 recentMessageCount 折叠历史消息。
refreshHooks() 可重复调用，每次调用前清空 systemHooks/systemPostHooks/regularPhaseHooks 再重新加载，替代原有的 initHooks()（仅能调用一次）
- 构建系统消息时，可用技能列表与已加载技能提示词均在主会话中过滤掉 `sessionAuth == SessionAuthType.CHILD` 的技能
- 子会话能力提示将工具与技能合并为单一 system message，格式为"子会话可使用以下工具：\n...\n子会话可使用以下技能：\n..."
## ChatDataProvider

聊天数据提供者接口（com.ghost616.agentbase.service.agent.ChatDataProvider），定义三个方法：getModelConfig(Long modelId) 按 ID 获取 ModelConfigData、updateSessionModelId(Long sessionId, Long modelId) 更新会话的模型 ID、getHooks() 获取所有已注册的 HookInvoker。用于解耦 ChatService 与具体数据访问层。

## ChatRequest

聊天请求 DTO（com.ghost616.agentbase.dto.chat.ChatRequest），从 platform-app 迁移而来，改包名为 com.ghost616.agentbase.dto.chat。包含字段：sessionId（必填）、content（必填）、modelId（可选）、thinking（可选）。
## AgentContextManager

AgentContextManager（非 @Component，通过 @Bean 注册）：注入 ContextDataProvider/SessionManager/ToolManager，管理会话上下文缓存 ConcurrentHashMap；提供 build(sessionId) 实例方法返回 Builder 内部类（支持 modelIdOverride 链式调用），Builder.build() 通过 cache.computeIfAbsent 使用 dataProvider 查询 agent/session 数据、toolManager 加载工具、sessionManager 获取历史消息，并在加载 skills 后遍历每条 SkillConfigDTO 的 skillTools，对 MCP_HTTP 类型工具调用 toolManager.expandMcpTools() 展开为 McpExpandedToolDTO 列表替换原始 DTO；保留 get/remove/addHistoryEntry 方法。
sendUserMessage 方法签名改为 Message 返回类型，透传给 AgentContextMutator 回调；方法体实现消息持久化（通过 sessionManager.messageSave()）并返回 Message 对象。
sendUserMessage 方法签名改为 Message 返回类型，通过 setter 注入 AgentMessageProxy 并委托给 proxy.sendUserMessage()；proxy 为 null 时回退为旧的直接保存 + 返回简单 Message。
Builder.doBuild() 在遍历 skills 展开 skillTools 的循环中，对每个加入 expandedTools 的 ToolConfigDTO 设置 sessionAuth = SessionAuthType.PARENT；MCP_HTTP 展开得到的 McpExpandedToolDTO 列表也逐个设置 sessionAuth = PARENT，使 skill 下的工具授权统一为父会话使用。
## ToolExecutionService

工具执行服务，非 Spring 组件。通过构造函数注入 ToolCallQueueManager/ToolManager/SystemToolManager/SessionManager/ChatService/AgentContextManager/ToolExecutionTracker/ObjectMapper。提供三个核心方法：
- executeTool(Long sessionId)：从队列获取下一个工具调用，解析调用器并异步执行，返回 ToolExecutionResult record(status/toolId/toolName/arguments/hasMore/message)
- getToolStatus(Long sessionId)：查询当前工具执行状态，返回 ToolStatusResult record(status/toolId/toolName/arguments/hasMore/result)
- continueAfterTools(Long sessionId)：检查无工具在执行后，持久化工具结果、添加历史记录、清理队列和跟踪器，构造 TOOL_CONTINUE_MARKER 请求并调用 chatService.chat() 返回 Flux
getToolStatus(Long sessionId, String toolId) toolId 为必传参数；continueAfterTools 已移除 "仍有工具正在执行中" 的阻塞检查。
## JsonMapper

公用 JSON 工具类（com.ghost616.agentbase.util.JsonMapper），final 类私有构造器，提供 public static final ObjectMapper MAPPER 实例。供 ChatService/ToolExecutionService 等组件直接引用，替代构造器注入方式。
## SessionManager

会话管理组件，提供 MessageSaveBuilder 链式构建消息保存、getMessages 历史消息查询和 rollbackToLastUserMessage 回退功能。MessageSaveBuilder.save() 方法在调用 dataProvider.saveMessage() 前对 sessionId/role/content 进行非空校验，任一为 null 时抛出 BusinessException(ErrorCode.PARAM_INVALID)。
## ConfigurableToolInvoker

ConfigurableToolInvoker 接口，继承 ToolInvoker，定义 setToolConfig(ToolConfigDTO) 方法。JavaToolInvoker 在加载工具实例后检测是否实现了该接口，若是则自动注入 ToolConfigDTO。
## ContextDataProvider

上下文数据提供者接口，定义 agent 配置、技能、会话变量等数据查询方法，以及子会话创建方法 createChildSession。
- createChildSession 方法参数 agentName 重命名为 sessionName
## AgentMessageProxy

AgentMessageProxy 消息代理类，注入 ChatService 和 ToolExecutionService。sendUserMessage(childSessionId, content, modelId) 同步代理：创建 ChatRequest 调用 chatService.chat() 收集 Flux<ServerSentEvent<ChatChunk>> 拼装 Message；检测 hasToolCalls 后循环调用 ToolExecutionService.executeTool() 等待完成 + continueAfterTools() 直到无工具调用，返回最终 assistant Message。
processChat 创建 Map<String, Integer> toolCallCounts 以 "toolName:arguments" 为 key 累计调用次数；processToolCalls 新增 Map 参数，在每次 executeTool 后合并计数，同一组合达到 5 次时 warn 日志并返回空 assistant Message 终止。保留 MAX_TOOL_ROUNDS 作为额外保障。
processChat 创建 Map<String, Integer> toolCallCounts 以 "toolName:arguments" 为 key 累计调用次数；processToolCalls 新增 Map 参数，在每次 executeTool 后合并计数，同一组合达到 5 次时 warn 日志并返回空 assistant Message 终止。保留 MAX_TOOL_ROUNDS 作为额外保障。

测试覆盖 7 个用例，含振荡保护边界（count >= 5）和 MAX_TOOL_ROUNDS 极限（round > 10），全部通过。
## SubSessionCallback

SubSessionCallback 函数式接口（com.ghost616.agentbase.service.agent.invoker），使用 @FunctionalInterface 注解，定义 execute(Long sessionId, String userMessage) 方法返回 Message，作为子会话消息处理的回调契约。
## ErrorCode

ErrorCode 枚举，包含系统、模型、工具、智能体、SKILL、会话等模块统一的错误码定义。已定义的系统错误码：SYSTEM_ERROR/PARAM_INVALID/NOT_FOUND/UNAUTHORIZED/DUPLICATE_KEY；模型错误码：MODEL_INVOKE_ERROR/MODEL_VERIFY_ERROR/MODEL_UNSUPPORTED/MODEL_NOT_FOUND/MODEL_ALREADY_EXISTS；工具错误码：TOOL_NOT_FOUND/TOOL_ALREADY_EXISTS/TOOL_SCHEMA_INVALID/TOOL_INVOKE_ERROR/TOOL_RUNTIME_NOT_FOUND/TOOL_EXECUTE_TIMEOUT/TOOL_EXECUTE_ERROR；智能体错误码：AGENT_NOT_FOUND/AGENT_ALREADY_EXISTS；SKILL 错误码：SKILL_NOT_FOUND/SKILL_ALREADY_EXISTS；会话错误码：SESSION_NOT_FOUND/SESSION_NO_USER_MESSAGE/SUB_SESSION_DATA_NOT_FOUND/CHILD_SESSION_NO_MESSAGES。
## SessionAuthType

SessionAuthType 枚举（com.ghost616.agentbase.enums.SessionAuthType），定义会话授权范围：ALL(0) 所有会话可用、PARENT(1) 父会话使用、CHILD(2) 子会话使用。使用 Integer code 字段标注 @EnumValue，提供 getCode/getDescription 方法。
