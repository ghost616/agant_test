# agent-base 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## SystemToolManager

系统工具管理器，非 Spring 组件。通过构造函数注入 SystemToolProvider 接口发现并注册系统工具，提供 getSystemTool(name) 按名称获取、getToolDefinitions() 构建带 _sys_ 前缀的 ToolDefinition 列表。
## ToolManager

getSessionTools 父会话 CHILD 副本修正：当 info.sessionAuth()==ALL 时，只基于原始未展开配置生成一份 CHILD 副本（ToolConfigDTO，非 McpExpandedToolDTO，invoker=null，mcpOriginalConfig 指向自身），而非对每个展开工具各生成一份。PARENT 副本保持对每个展开 McpExpandedToolDTO 生成一份不变。子会话逻辑不变。
createInvoker 方法新增 CUSTOM 分支：当 toolConfig.getToolType() 为 CUSTOM 时，从 AgentComponentRegistry 获取 CustomToolInvokerProvider 调用 getInvoker(toolConfig) 创建调用器；若 provider 为 null 则抛出 UnsupportedOperationException。

createInvoker 方法 CUSTOM 分支：当 toolConfig.getToolType() 为 CUSTOM 时，从 dataProvider.getCustomInvoker(toolConfig) 获取 CustomToolInvoker 创建调用器；若返回 null 则抛出 UnsupportedOperationException。
新增 public List<Map<String, Object>> getBuiltinTools(String modelId) 方法：ensureInitialized 后委托 dataProvider.getBuiltinTools(modelId) 返回内置工具列表，供 ChatService 在 Responses 系列流程构建模型请求时使用。

## ToolCallQueueManager

构造函数改为接收 AgentComponentRegistry，移除内部 ConcurrentHashMap。通过 registry.getToolExecutionProvider() 获取 ToolExecutionProvider，enqueue/poll/peek/hasPending/clear 五个队列方法全部委派给 provider，public API 签名不变。
## ToolExecutionProvider

ToolExecutionProvider 接口（com.ghost616.agentbase.service.agent.ToolExecutionProvider），定义工具调用队列和执行追踪的统一契约。包含 enqueue/poll/peek/hasPending/clearQueue 五个队列方法与 updateExecution/clearTracking/getCurrentExecution/getAndClearResults 四个执行追踪方法。返回类型引用 ToolExecutionTracker.ToolExecutionStatus 和 ToolExecutionTracker.ToolResult 内部 record。平台实现类 DefaultToolExecutionProvider（com.ghost616.platform.service.agent）内部持有队列 ConcurrentHashMap 和追踪 ConcurrentHashMap，承担原 ToolCallQueueManager 和 ToolExecutionTracker 的 Map 存储职责。
## AgentComponentRegistry

AgentComponentRegistry（com.ghost616.agentbase.core.AgentComponentRegistry），中央组件注册表，非 Spring 组件。持有所有 Provider/Manager 组件的 @Setter 注入字段，包含 contextDataProvider/messageDataProvider/toolDataProvider/chatDataProvider/modelInvokerDataProvider/systemToolProvider/modelInvokerFactory/toolManager/toolCallQueueManager/systemToolManager/sessionManager/agentContextManager/modelInvokerManager/toolExecutionTracker/toolExecutionProvider/messageSender/hookManager。每个 getter 方法通过 requireInitialized 守卫确保组件已初始化后返回，messageSender 可为 null。lastResponseId 持久化已于 2026-07-31 由 SessionDataProvider 迁移至 ContextDataProvider（sessionDataProvider 字段与 getSessionDataProvider() 已移除）。
## HookInvoker / SystemHook / SystemPostHook


## HookManager

HookManager 抽取自 ChatService/ToolExecutionService 的公共 HOOK 管理基础设施。内部持有 AgentComponentRegistry registry 字段、systemHooks / systemPostHooks / regularPhaseHooks 三个集合。构造函数注入 AgentComponentRegistry，提供 refreshHooks() 无参方法（内部通过 registry.getChatDataProvider().getHooks() 获取 HookInvoker 列表后按类型分类）、triggerHooks(HookPhase, AgentExecutionContext, HookData) 触发全局阶段钩子、triggerSessionHooks(String sessionId, HookPhase, AgentExecutionContext, HookData) 通过 ChatDataProvider.getHooks(sessionId) 加载会话专属 HOOK 并按 phase 匹配执行、executePostHooks(AgentExecutionContext, HookData) 触发后置钩子共四个方法。
triggerHooks / triggerSessionHooks / executePostHooks 中每个 h.execute(ctx, data) 调用均包裹 try-catch，异常时以 WARN 级别日志记录失败信息（含 hook 类名），继续执行后续 hook，不中断整体流程。
triggerSessionHooks 按与 triggerHooks 相同的 phase 匹配逻辑：普通 HOOK 直接执行，SystemHook 按 index 排序后执行，SystemPostHook 跳过（归 executePostHooks 处理）。
## HookData

HookData 数据载体类，包含 ChatChunk chatChunk 和 ToolHookContext toolContext 两个 final 不可变字段。提供两个独立构造器：HookData(ChatChunk) 和 HookData(ToolHookContext)，分别设置对应字段。无 setter，创建后不可修改。由 @Getter @ToString @EqualsAndHashCode 生成访问方法与对象方法。
## HookInvoker
execute 方法签名由 execute(AgentExecutionContext, ChatChunk) 改为 execute(AgentExecutionContext, HookData)，调用方通过 new HookData(chunk) 包装 ChatChunk 传入。

## MessageSavePostHook

从 platform-app 迁移而来。消息保存后置 HOOK，在 AFTER_MESSAGE_RECEIVE 阶段缓存流式块，收到 finishReason=stop 时拼装消息调用 sessionManager.messageSave() 持久化，并通过 toolCallQueueManager.enqueue() 入队工具调用。已去掉 @Component/@RequiredArgsConstructor，构造函数改为注入 AgentComponentRegistry，execute 开头惰性初始化 contextDataProvider/sessionManager/agentContextManager/toolCallQueueManager 四个字段（registry.getContextDataProvider()/getSessionManager()/getAgentContextManager()/getToolCallQueueManager()）。
- finishReason=stop 时从 chunk.getUsage() 获取 UsageInfo，通过 .usage(usage) 传入 messageSave 链，同时传入 HistoryEntry 构造器
- finishReason=stop 分支中，ctx.getLastResponseId() 非空时调用 contextDataProvider.updateLastResponseId(sessionId, lastResponseId) 持久化会话最近响应 ID（仅当非空才写入）
- SessionBuffer 新增 List&lt;ChatChunk.WebSearchCall&gt; webSearchCalls 和 List&lt;ChatChunk.CustomToolCall&gt; customToolCalls 两个累积字段；非 stop 分支中 chunk.getWebSearchCall()/getCustomToolCall() 非空时累积到对应列表
- finishReason=stop 分支中将累积列表整体通过 toWebSearchCallData/toCustomToolCallData 私有方法转为 List&lt;MessageDataProvider.WebSearchCallData&gt;/List&lt;MessageDataProvider.CustomToolCallData&gt;，经 .webSearchCall()/.customToolCall() 链传入 messageSave，并将累积列表（空时传 Collections.emptyList()）传入 HistoryEntry 构造器
## ToolExecutionTracker

非 Spring 组件（已去掉 @Component），保留 @Slf4j。构造函数改为接收 AgentComponentRegistry，通过 registry.getToolExecutionProvider() 获取 ToolExecutionProvider，所有 Map 操作（setExecuting/setDone/setFailed/clear/getCurrentExecution/getAndClearResults）委派给 provider。内部 record ToolExecutionStatus（status/toolId/toolName/arguments）和 ToolResult（toolId/toolName/arguments/result）保持不变。
## ModelConfigData

ModelConfigData record（com.ghost616.agentbase.dto.model.ModelConfigData），包含字段：String id, String apiKey, String baseUrl, String modelName, Double temperature, Integer maxTokens, String platformType, String requestType。requestType 表示模型请求类型（如 "responses" 走 Responses API），可为 null，ChatService 依据该字段分发调用流程。
新增实例方法 isResponsesType()：委托 RequestType.isResponses(requestType) 判断当前模型配置是否走 Responses 系列请求（responses / responses_stateless 均返回 true）。
## ModelInvokerFactory

ModelInvokerFactory 接口（com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory），定义 createInvoker(ModelConfigData) 方法，返回 ModelInvoker。用于解耦 ModelInvokerManager 与具体 invoker 创建逻辑。
## ModelInvokerManager

从 platform-app 迁移而来。已去掉 @Component/@RequiredArgsConstructor 及 RestClient.Builder/WebClient.Builder 字段，改为构造函数注入 ModelInvokerFactory。createInvoker 委托给 factory.createInvoker(config)。getInvoker 参数改为 ModelConfigData，通过 config.id() 缓存。提供 register/evict/clear/cacheSize/getInvokerById 方法。
## ChatService

ChatService 聊天服务，非 Spring 组件。构造函数接收 AgentComponentRegistry，通过 registry 延迟获取 AgentContextManager/SessionManager/ModelInvokerManager/SystemToolManager/ChatDataProvider。chat(ChatRequest) 为入口路由方法：先构建会话上下文、保存用户消息、更新模型 ID、获取 ModelConfigData 并触发 SESSION_START HOOK，再基于 RequestType 枚举对 configData.requestType() 显式三路分发：RESPONSES 走 chatViaResponses()（有状态）、RESPONSES_STATELESS 走 chatViaResponsesStateless()（无状态）、COMPLETIONS/null/其他 走 chatViaChatCompletions()。

chatViaChatCompletions() 承载原 chat() 核心逻辑（构建 system 消息、拼接历史、调用 invoker.invokeStream）。

chatViaResponses()（有状态）：previousResponseId 优先取自会话上下文 lastResponseId（AgentExecutionContext.lastResponseId，由流式 response.completed 携带的 responseId 写入），lastResponseId 为 null 时回退到 API 请求的 previousResponseId；input 只传增量消息（从最后一个 user 角色历史条目起，即本轮 user + 返回的 assistant/tool），不重复发送此前各轮的 assistant 消息。

chatViaResponsesStateless()（无状态）：不传 previousResponseId，input 传全量历史 messages（不含 system role），与 chat completions 全量一致。

两条 responses 流程均将 system prompt 与动态技能提示词拼入 ChatRequest.instructions 字段，input 不含 system role。三路流程共用 buildContextSystemInfo（构建技能/子会话 system 消息及 filteredLoadedSkills）、buildToolDefinitions（工具列表构建）、buildInstructions、buildMessageFromEntry、buildFullMessages/buildIncrementalMessages、filterAndFold、toSseStream（流式 SSE + HOOK 拦截 + 捕获 chunk.responseId 写入 context.lastResponseId）。foldMessageGroups 按 recentMessageCount 折叠历史消息（支持无 system 前缀输入）。
新增 ToolManager toolManager 字段，ensureInitialized 中从 registry.getToolManager() 懒加载。chatViaResponses/chatViaResponsesStateless 构建模型 ChatRequest 前调用 toolManager.getBuiltinTools(configData.id()) 并设置 .builtinTools(...)，为 Responses 系列请求携带模型内置工具配置。
## ChatDataProvider

聊天数据提供者接口（com.ghost616.agentbase.service.agent.ChatDataProvider），定义四个方法：getModelConfig(String modelId) 按 ID 获取 ModelConfigData、updateSessionModelId(String sessionId, String modelId) 更新会话的模型 ID、getHooks() 获取所有已注册的 HookInvoker、getHooks(String sessionId) 按会话 ID 获取对应的 HookInvoker 列表。用于解耦 ChatService 与具体数据访问层。
## ChatRequest

聊天请求 DTO（com.ghost616.agentbase.dto.chat.ChatRequest），从 platform-app 迁移而来，改包名为 com.ghost616.agentbase.dto.chat。包含字段：sessionId（必填）、content（必填）、modelId（可选）、thinking（可选）、previousResponseId（可选，Responses API 多轮续接时透传给模型请求）。
## AgentContextManager

AgentContextManager（非 @Component，通过 @Bean 注册）：注入 ContextDataProvider/SessionManager/ToolManager，管理会话上下文缓存 ConcurrentHashMap&lt;String, AgentSessionContext&gt;；提供 build(sessionId) 实例方法返回 Builder 内部类（支持 modelIdOverride 链式调用），Builder.build() 通过 cache.computeIfAbsent 使用 dataProvider 查询 agent/session 数据、toolManager 加载工具、sessionManager 获取历史消息，并在加载 skills 后遍历每条 SkillConfigDTO 的 skillTools，对 MCP_HTTP 类型工具调用 toolManager.expandMcpTools() 展开为 McpExpandedToolDTO 列表替换原始 DTO；保留 get/remove/addHistoryEntry 方法。
sendUserMessage 方法签名改为 Message 返回类型，透传给 AgentContextMutator 回调；方法体实现消息持久化（通过 sessionManager.messageSave()）并返回 Message 对象。
sendUserMessage 方法签名改为 Message 返回类型，通过 setter 注入 AgentMessageProxy 并委托给 proxy.sendUserMessage()；proxy 为 null 时回退为旧的直接保存 + 返回简单 Message。
Builder.doBuild() 在遍历 skills 展开 skillTools 的循环中，对每个加入 expandedTools 的 ToolConfigDTO 设置 sessionAuth = SessionAuthType.PARENT；MCP_HTTP 展开得到的 McpExpandedToolDTO 列表也逐个设置 sessionAuth = PARENT，使 skill 下的工具授权统一为父会话使用。
- doBuild() 中构建 HistoryEntry 时从 msg.usage() 获取 UsageInfo 传入构造器
- refreshHistory(String sessionId)：调用 dataProvider.getLatestMessages() 获取最新消息，通过 convertMessagesToHistory() 转为 HistoryEntry 列表，经 mutator.refreshHistory() 更新缓存
- refreshSessionVariables(String sessionId)：调用 dataProvider.getLatestSessionVariables()，通过 mutator.refreshSessionVariables() 更新
- refreshConversationVariables(String sessionId)：调用 dataProvider.getLatestConversationVariables()，通过 mutator.refreshConversationVariables() 更新
- refreshChildSessions(String sessionId)：调用 dataProvider.getLatestChildSessions()，通过 mutator.refreshChildSessions() 更新
- convertMessagesToHistory(List&lt;MessageDTO&gt;) 从 doBuild 中提取的私有方法，复用消息转 HistoryEntry 逻辑
- 所有刷新方法在缓存中无对应 sessionId 的上下文时静默返回

### AgentContextMutator 消息发送
AgentContextMutator 在 6 个操作点通过注入的 MessageSender 发送消息：addHistoryEntry() 发送 HistoryMessage；putSessionVariable()/removeSessionVariable() 发送 VariableMessage(scope="SESSION")；putConversationVariable()/removeConversationVariable() 发送 VariableMessage(scope="CONVERSATION")；createChildSession() 发送 ChildCreateSession；sendUserMessage() 发送 ChildMessageEvent。所有发送操作在 messageSender 为 null 时静默跳过。

### AgentContextManager 消息处理器
AgentContextManager 提供 3 个 public handler 方法供外部系统在收到消息后调用：handleChildCreateSession(ChildCreateSession) 将新子会话添加到父会话缓存的 childSessions 列表中；handleHistoryMessage(HistoryMessage) 将 HistoryEntry 追加到对应会话的 history 列表；handleVariableMessage(VariableMessage) 根据 scope 复制当前变量 Map 并应用 PUT/REMOVE 操作后刷新。

### 父子会话变量委托
injectVariableCallbacks() 方法在子会话上下文中，将 sessionVarPutCallback/sessionVarRemoveCallback 直接指向父会话上下文的 putSessionVariable/removeSessionVariable，实现子会话变量读写直接委托给父会话，不经过 MessageSender。ConversationVariable 同理。
- AgentExecutionContext.HistoryEntry record 新增 List&lt;ChatChunk.WebSearchCall&gt; webSearchCall 和 List&lt;ChatChunk.CustomToolCall&gt; customToolCall 字段（类型复用 ChatChunk 内部类）；convertMessagesToHistory 通过 toWebSearchCall/toCustomToolCall 私有方法将 MessageDTO 的 List&lt;WebSearchCallData&gt;/List&lt;CustomToolCallData&gt; 转为 ChatChunk List 后传入 HistoryEntry
## ToolExecutionService

工具执行服务，非 Spring 组件。构造函数改为接收 (AgentComponentRegistry, ChatService)，通过 registry 延迟获取 ToolCallQueueManager/ToolManager/SystemToolManager/SessionManager/AgentContextManager/ToolExecutionTracker。提供三个核心方法：executeTool(String sessionId) 从队列获取下一个工具调用，解析调用器并异步执行；getToolStatus(String sessionId, String toolId) 查询当前工具执行状态（toolId 为必传参数）；continueAfterTools(String sessionId) 检查无工具在执行后，持久化工具结果、添加历史记录、清理队列和跟踪器，构造 TOOL_CONTINUE_MARKER 请求并调用 chatService.chat()。
## ToolHookContext
ToolHookContext 数据载体（@Data @AllArgsConstructor @NoArgsConstructor），包含 toolCallId / toolName / arguments / result 四个字段，用于在 BEFORE_TOOL_CALL 和 AFTER_TOOL_CALL 阶段向 HOOK 传递工具执行上下文。

## HookData
新增 ToolHookContext toolContext 字段，可为 null。聊天阶段（chat 调用）为 null，工具调用阶段携带工具执行上下文。

## ToolExecutionService HOOK 基础设施
新增与 ChatService 相同的 HOOK 基础设施：systemHooks / systemPostHooks / regularPhaseHooks 三个集合、refreshHooks / triggerHooks / executePostHooks 三个方法。executeTool 中在 CompletableFuture.supplyAsync 前触发 BEFORE_TOOL_CALL，在 setDone 后触发 AFTER_TOOL_CALL，均通过 HookData(toolContext) 传递工具上下文。

## JsonMapper

公用 JSON 工具类（com.ghost616.agentbase.util.JsonMapper），final 类私有构造器，提供 public static final ObjectMapper MAPPER 实例。供 ChatService/ToolExecutionService 等组件直接引用，替代构造器注入方式。
## SessionManager

会话管理组件，提供 MessageSaveBuilder 链式构建消息保存、getMessages 历史消息查询和 rollbackToLastUserMessage 回退功能。MessageSaveBuilder.save() 方法在调用 dataProvider.saveMessage() 前对 sessionId/role/content 进行非空校验，任一为 null 时抛出 BusinessException(ErrorCode.PARAM_INVALID)。
- MessageSaveBuilder 新增 UsageInfo usage 字段和 .usage(UsageInfo) 链式方法，save() 时透传给 dataProvider.saveMessage() 的 usage 参数
- 会话级 lastResponseId 持久化已于 2026-07-31 迁移至 ContextDataProvider，SessionDataProvider 相关委托方法（updateLastResponseId/getLastResponseId/updateSessionThinking）已移除
- MessageSaveBuilder 新增 List&lt;MessageDataProvider.WebSearchCallData&gt; webSearchCall、List&lt;MessageDataProvider.CustomToolCallData&gt; customToolCall 字段与链式方法，save() 时透传给 dataProvider.saveMessage()
## ConfigurableToolInvoker

ConfigurableToolInvoker 接口，继承 ToolInvoker，定义 setToolConfig(ToolConfigDTO) 方法。JavaToolInvoker 在加载工具实例后检测是否实现了该接口，若是则自动注入 ToolConfigDTO。
## ContextDataProvider

上下文数据提供者接口，定义 agent 配置、技能、会话变量等数据查询方法，以及子会话创建方法 createChildSession。
- AgentContextData record 新增 String lastResponseId 字段（最后一个参数），承载会话最近一次模型响应 ID（Responses API 有状态续接时作为 previousResponseId），由 loadAgentContext 从持久层填充，AgentContextManager.doBuild 注入上下文
- 新增 updateLastResponseId(String sessionId, String lastResponseId) 方法，持久化会话最近响应 ID，由 MessageSavePostHook 在消息保存完成后调用
- createChildSession 方法参数 agentName 重命名为 sessionName
- getLatestMessages(String sessionId) → List<MessageDTO>：获取会话全部消息
- getLatestSessionVariables(String sessionId) → Map<String, String>：获取全部会话变量
- getLatestConversationVariables(String sessionId) → Map<String, String>：获取全部对话变量
- getLatestChildSessions(String sessionId) → List<ChildSession>：获取全部子会话列表
## AgentMessageProxy

AgentMessageProxy 消息代理类，注入 ChatService 和 ToolExecutionService。sendUserMessage(childSessionId, content, modelId) 同步代理：创建 ChatRequest 调用 chatService.chat() 收集 Flux<ServerSentEvent<ChatChunk>> 拼装 Message；检测 hasToolCalls 后循环调用 ToolExecutionService.executeTool() 等待完成 + continueAfterTools() 直到无工具调用，返回最终 assistant Message。
processChat 创建 Map<String, Integer> toolCallCounts 以 "toolName:arguments" 为 key 累计调用次数；processToolCalls 新增 Map 参数，在每次 executeTool 后合并计数，同一组合达到 5 次时 warn 日志并返回空 assistant Message 终止。保留 MAX_TOOL_ROUNDS 作为额外保障。
processChat 创建 Map<String, Integer> toolCallCounts 以 "toolName:arguments" 为 key 累计调用次数；processToolCalls 新增 Map 参数，在每次 executeTool 后合并计数，同一组合达到 5 次时 warn 日志并返回空 assistant Message 终止。保留 MAX_TOOL_ROUNDS 作为额外保障。

测试覆盖 7 个用例，含振荡保护边界（count >= 5）和 MAX_TOOL_ROUNDS 极限（round > 10），全部通过。
## SubSessionCallback

SubSessionCallback 函数式接口（com.ghost616.agentbase.service.agent.invoker），使用 @FunctionalInterface 注解，定义 execute(String sessionId, String userMessage, Boolean thinking) 方法返回 Message，作为子会话消息处理的回调契约。thinking 参数表示是否启用思考模式，可为 null 表示使用默认行为。
## ErrorCode

ErrorCode 枚举，包含系统、模型、工具、智能体、SKILL、会话、评估等模块统一的错误码定义。已定义的系统错误码：SYSTEM_ERROR/PARAM_INVALID/NOT_FOUND/UNAUTHORIZED/DUPLICATE_KEY；模型错误码：MODEL_INVOKE_ERROR/MODEL_VERIFY_ERROR/MODEL_UNSUPPORTED/MODEL_NOT_FOUND/MODEL_ALREADY_EXISTS；工具错误码：TOOL_NOT_FOUND/TOOL_ALREADY_EXISTS/TOOL_SCHEMA_INVALID/TOOL_INVOKE_ERROR/TOOL_RUNTIME_NOT_FOUND/TOOL_EXECUTE_TIMEOUT/TOOL_EXECUTE_ERROR；智能体错误码：AGENT_NOT_FOUND/AGENT_ALREADY_EXISTS；SKILL 错误码：SKILL_NOT_FOUND/SKILL_ALREADY_EXISTS；会话错误码：SESSION_NOT_FOUND/SESSION_NO_USER_MESSAGE/SUB_SESSION_DATA_NOT_FOUND/CHILD_SESSION_NO_MESSAGES；评估错误码：EVALUATION_NOT_FOUND/EVALUATION_ALREADY_EXISTS/EVALUATION_BENCHMARK_NO_USER_MESSAGE/EVALUATION_EXECUTION_STATUS_NOT_FOUND/EVALUATION_SESSION_NOT_CREATED/EVALUATION_RESULT_GENERATE_ERROR；智能体评估错误码：AGENT_EVALUATION_NOT_FOUND/AGENT_EVALUATION_ALREADY_EXISTS。
## SessionAuthType

SessionAuthType 枚举（com.ghost616.agentbase.enums.SessionAuthType），定义会话授权范围：ALL(0) 所有会话可用、PARENT(1) 父会话使用、CHILD(2) 子会话使用。使用 Integer code 字段标注 @EnumValue，提供 getCode/getDescription 方法。

## MessageSender

MessageSender 接口（com.ghost616.agentbase.sendmessage.MessageSender），可插拔消息发送扩展点。定义 void send(MessageDefinition message) 单一方法。当前通过 AgentAssembler 传入 null，供外部集成者实现具体传输（WebSocket/SSE/消息队列等）。
## MessageDefinition

MessageDefinition 接口（com.ghost616.agentbase.sendmessage.MessageDefinition），所有消息类型的顶层契约。定义 String getMessageName() 方法。
## MessageName

MessageName 常量类，定义 5 种消息类型名称：HISTORY_MESSAGE、SESSION_VARIABLE、CONVERSATION_VARIABLE、CHILD_SESSION、CHILD_MESSAGE。
## SessionMessage

SessionMessage 抽象类，实现 MessageDefinition，新增 String sessionId 字段，作为所有会话级消息的基类。
## HistoryMessage

HistoryMessage 消息类，继承 SessionMessage，messageName=HISTORY_MESSAGE。携带 HistoryEntry historyEntry 字段，由 AgentContextMutator.addHistoryEntry() 在添加历史记录时触发发送。
## VariableMessage

VariableMessage 消息类，继承 SessionMessage。messageName 为 SESSION_VARIABLE 或 CONVERSATION_VARIABLE，通过 scope 字段区分。携带 scope（"SESSION"/"CONVERSATION"）、key、value、operation（"PUT"/"REMOVE"）字段。由 AgentContextMutator 的 putSessionVariable/removeSessionVariable 发送 SESSION 作用域消息，putConversationVariable/removeConversationVariable 发送 CONVERSATION 作用域消息。messageSender 为 null 时静默跳过发送。
## ChildCreateSession

ChildCreateSession 消息类，继承 SessionMessage，messageName=CHILD_SESSION。携带 String parentSessionId 和 ChildSession childSession 字段。由 AgentContextMutator.createChildSession() 在创建子会话后触发。
## ChildMessageEvent

ChildMessageEvent 消息类，继承 SessionMessage，messageName=CHILD_MESSAGE。携带 String childSessionId、String content、String modelId、Boolean thinking、Message result 字段。由 AgentContextMutator.sendUserMessage() 在子会话消息完成时触发。
## 资源文件

从 platform-app 迁移而来。agent-base/src/main/resources/agent/ 目录下包含两个工具运行桥接脚本：
- _runner.py：Python 工具运行桥接，供 PythonToolInvoker 调用
- _runner.ts：TypeScript 工具运行桥接，供 TypeScriptToolInvoker 调用
## MessageDataProvider

消息数据提供者接口（com.ghost616.agentbase.service.agent.MessageDataProvider），定义消息保存、查询、回退方法。内部 record MessageDTO 包含字段：String id, String sessionId, String role, String content, String reasoning, String toolCallId, Integer sequenceNum, LocalDateTime createTime, String toolResult, List&lt;ToolCallData&gt; toolCalls, UsageInfo usage, Boolean rollback（默认 null）。内部 record ToolCallData 包含字段：String toolCallId, String toolCallName, String toolCallArguments。
- ToolCallData record 新增 String type 字段（默认 "function"，保留 3 参构造器以兼容旧调用）；新增 WebSearchCallData（itemId/outputIndex/results，results 为 List&lt;WebSearchResultData&gt;，每项含 title/url/snippet）和 CustomToolCallData（itemId/outputIndex/input/output）两个 record
- saveMessage 签名新增 List&lt;WebSearchCallData&gt; webSearchCall、List&lt;CustomToolCallData&gt; customToolCall 两个参数（置于 usage 之后）；MessageDTO record 末尾新增 webSearchCall、customToolCall 两个 List 字段
## SessionVariableSystemTool / ConversationVariableSystemTool

两个系统工具类，实现 SystemTool 接口，用于在会话过程中管理变量。

SessionVariableSystemTool（工具名：session_variable）：管理会话变量，支持 add/get/remove 三种操作，分别调用 ctx.putSessionVariable/getSessionVariable/removeSessionVariable。

ConversationVariableSystemTool（工具名：conversation_variable）：管理对话变量，支持 add/get/remove 三种操作，分别调用 ctx.putConversationVariable/getConversationVariable/removeConversationVariable。

两个工具的参数结构一致：action（add/get/remove）、key（变量名）、value（add 时必填）。返回值均为 JSON 格式。
## ToolType

新增 CUSTOM("CUSTOM", "自定义实现") 枚举值，用于支持外部自定义工具调用器实现。
## CustomToolInvoker

CustomToolInvoker（com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker），抽象类实现 ToolInvoker 接口。持有 protected final ToolConfigDTO toolConfig 属性，通过构造函数注入。供外部实现类继承并实现 execute 方法。
## CustomToolInvokerProvider
## ToolDataProvider

ToolDataProvider（com.ghost616.agentbase.service.agent.ToolDataProvider），工具数据提供者接口，定义 getSessionToolIds/getToolById/getSkillToolIds/getCustomInvoker 方法。getSkillToolIds 返回类型从 List<String> 改为 List<SkillToolInfo>，SkillToolInfo 为内部 record（skillId/sessionAuth/toolIds），按技能分组并携带授权类型。getCustomInvoker(ToolConfigDTO) 返回 CustomToolInvoker，替代原 CustomToolInvokerProvider 接口能力。
新增 getBuiltinTools(String modelId) 方法，返回 List<Map<String, Object>>（内置工具列表，如 web_search，每项为工具配置键值对），按模型 ID 查询模型侧内置工具配置。

ToolDataProvider（com.ghost616.agentbase.service.agent.ToolDataProvider），工具数据提供者接口，定义 getSessionToolIds/getToolById/getSkillToolIds/getCustomInvoker/getBuiltinTools 方法。getSkillToolIds 返回类型从 List<String> 改为 List<SkillToolInfo>，SkillToolInfo 为内部 record（skillId/sessionAuth/toolIds），按技能分组并携带授权类型。getCustomInvoker(ToolConfigDTO) 返回 CustomToolInvoker，替代原 CustomToolInvokerProvider 接口能力。
## 对话模型请求/响应 DTO

对话模型请求 DTO（com.ghost616.agentbase.dto.model.ChatRequest），承载发送给模型服务的请求：messages（对话消息）、tools（工具定义）、temperature、maxTokens、model、thinking、previousResponseId（上一轮响应 ID，Responses API 多轮续接）、instructions（系统级指令，Responses API 下存放 system prompt 与动态技能提示词）。对话响应 DTO（com.ghost616.agentbase.dto.model.ChatResponse）新增 responseId 字段，为模型返回的响应 ID，供下一轮续接时作为 previousResponseId 使用。流式片段 ChatChunk 新增 responseId 字段，由 OpenAIResponsesInvoker 在解析 response.completed 事件时从 response.id 写入，ChatService.toSseStream 捕获后存入会话上下文 lastResponseId 供有状态续接。

- dto.model.ChatRequest 新增 List<Map&lt;String, Object&gt;> builtinTools 字段（内置工具列表，如 web_search，每项为工具配置键值对）
- ChatChunk 新增 webSearchCall、customToolCall 两个字段，分别引用独立类 WebSearchCall（itemId/outputIndex/results，results 为 List 每项含 title/url/snippet）和 CustomToolCall（itemId/outputIndex/input/output）
- WebSearchCall（含内部类 WebSearchResult：title/url/snippet）与 CustomToolCall 已由原 ChatChunk 静态内部类抽取为独立 DTO 类（com.ghost616.agentbase.dto.model），供 ChatChunk、AgentExecutionContext.HistoryEntry、MessageSavePostHook、AgentContextManager 共同引用
## 模型请求类型

RequestType 枚举（com.ghost616.agentbase.enums.RequestType），定义模型请求分发方式。包含 RESPONSES("responses", "Responses（有状态）")、RESPONSES_STATELESS("responses_stateless", "Responses（无状态）")、COMPLETIONS("completions", "Chat Completions") 三个值，提供 getCode()/getDescription() 方法及静态 isResponses(String code) 判断（仅对 responses 与 responses_stateless 返回 true，排除 completions）。ModelConfigData.isResponsesType() 委托该方法。
## 会话数据访问层

会话数据访问层已于 2026-07-31 重构：SessionDataProvider 接口被删除，会话级 lastResponseId 的持久化与查询职责迁移至 ContextDataProvider（新增 updateLastResponseId 方法 + AgentContextData.lastResponseId 字段）。lastResponseId 由 AgentContextManager.doBuild 从 loadAgentContext 读取注入上下文（AgentContextMutator.setLastResponseId），流式过程中由 ChatService.toSseStream 通过 contextMutator.setLastResponseId 捕获 chunk.responseId，消息保存完成后由 MessageSavePostHook 通过 contextDataProvider.updateLastResponseId 写回持久层。原 SessionDataProvider 三个方法（updateLastResponseId/getLastResponseId/updateSessionThinking）已随接口删除。
## 会话数据访问层

SessionDataProvider（com.ghost616.agentbase.service.agent.SessionDataProvider），会话数据提供者接口，定义会话级数据的持久化与查询契约，供 SessionManager 懒加载引用，与平台具体数据访问层（platform-app 的 DefaultSessionDataProvider）解耦。包含三个方法：
- updateLastResponseId(String sessionId, String lastResponseId)：更新会话最近一次模型响应 ID（Responses API 有状态续接时作为 previousResponseId）
- getLastResponseId(String sessionId)：查询会话最近一次响应 ID，无则返回 null
- updateSessionThinking(String sessionId, Boolean thinking)：更新会话思考模式开关
