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

AgentComponentRegistry（com.ghost616.agentbase.core.AgentComponentRegistry），中央组件注册表，非 Spring 组件。持有所有 Provider/Manager 组件的 @Setter 注入字段，包含 contextDataProvider/messageDataProvider/toolDataProvider/chatDataProvider/modelInvokerDataProvider/systemToolProvider/modelInvokerFactory/toolManager/toolCallQueueManager/systemToolManager/sessionManager/agentContextManager/modelInvokerManager/toolExecutionTracker/toolExecutionProvider/messageSender/hookManager/agentLog/chatDataCacheManager。每个 getter 方法通过 requireInitialized 守卫确保组件已初始化后返回，messageSender 与 agentLog 可为 null。lastResponseId 持久化已于 2026-07-31 由 SessionDataProvider 迁移至 ContextDataProvider（sessionDataProvider 字段与 getSessionDataProvider() 已移除）。

agentLog 字段（@Setter）与 getAgentLog() getter 已添加，参照 MessageSender 模式：getter 直接返回、不校验 null，供外部集成者注入智能体日志实现。

chatDataCacheManager 字段（@Setter）与 getChatDataCacheManager() getter 已添加，参照 messageSender/agentLog 可选模式：getter 直接返回字段值、不抛 requireInitialized 异常，供外部集成者注入聊天数据缓存管理器。
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

ModelConfigData record（com.ghost616.agentbase.dto.model.ModelConfigData），包含字段：String id, String apiKey, String baseUrl, String modelName, Double temperature, Integer maxTokens, String platformType, ModelType modelType, String requestType。modelType 表示模型类型（LLM/EMBEDDINGS），位于 platformType 之后、requestType 之前，可为 null；requestType 表示模型请求类型（如 "responses" 走 Responses API），可为 null，ChatService 依据该字段分发调用流程。
保留 8 参兼容构造器（省略 modelType），默认 modelType=ModelType.LLM，与全参构造器共存，兼容既有调用方。
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
chatViaChatCompletions 构建模型 ChatRequest 时同样调用 toolManager.getBuiltinTools(configData.id()) 并设置 .builtinTools(...)，与 Responses 系列流程保持一致。
已加载技能 prompt 消息位置迁移（KV Cache 优化）：ContextSystemInfo record 新增 loadedSkillMessages 字段。buildContextSystemInfo 中已加载技能提示词消息（"以下技能已加载，请按照其提示词指导执行任务"）由 systemMessages 移入 loadedSkillMessages，systemMessages 仅保留可用技能列表与子会话权限说明。chatViaChatCompletions 在 filterAndFold 后调用 insertLoadedSkillMessages：找到最后一条 role=user 的消息索引，在其之前插入 loadedSkillMessages（无 user 消息时追加到列表末尾），使系统提示词前缀在会话中保持稳定。buildInstructions 方法签名新增 List<Message> loadedSkillMessages 参数，在 instructions 末尾追加其内容，chatViaResponses/chatViaResponsesStateless 调用处传入 systemInfo.loadedSkillMessages()。
foldMessageGroups 批量折叠 + 锚点展开（KV Cache 优化）：新增常量 DEFAULT_FOLD_INTERVAL=10 与 HISTORY_GROUP_PREFIX="【历史消息组"。foldMessageGroups 按 user 消息边界分组、system 前缀单独提出；groups.size()<=recentCount 或 foldedCount==0（foldedCount=max(0,(groups.size()-recentCount)/interval)*interval）时直接返回不折叠；旧区 0..foldedCount-1 折叠为 user 首条+占位 assistant「此为历史消息索引为N，如果想要展开请调用历史消息工具」，近端区完整展开；读取 _sys_his_msgs_index 对话变量，对 <foldedCount 的请求展开索引组装【历史消息组{N}】system 消息（每 entry 一行 role: content）并插入最后一条 user 消息之前。chatViaChatCompletions 最终顺序：[SYSTEM 底座prompt][SYSTEM 系统信息][折叠区+近端区][SYSTEM loadedSkills][SYSTEM 锚点展开][USER 当前消息]；insertLoadedSkillMessages 优先插入到首个锚点消息之前，无锚点回退最后一条 user 之前，无 user 追加列表末尾。
FoldResult 拆分 + 锚点修复：foldMessageGroups/filterAndFold 返回 FoldResult(messages/anchorMessages)，锚点 system 消息不再嵌入 messages 列表而是独立返回；chatViaChatCompletions 拆包后先 insertLoadedSkillMessages（最后 user 前）再 insertAnchorMessages（最后 user 前），共用 findLastUserIndex，最终顺序 [SYSTEM 底座][SYSTEM 系统信息][折叠区+近端区][SYSTEM loadedSkills][SYSTEM 锚点][USER 当前]；chatViaResponses/chatViaResponsesStateless 拆包 FoldResult 并将 anchorMessages 传入 buildInstructions（在 loadedSkillMessages 之后追加），修复 Responses API 锚点未纳入 instructions 的问题；buildHistoryGroupMessage 补全信息：assistant 含非空 reasoning 且 toolCalls 非空时输出 reasoning 行、toolCalls 逐条输出 tool_call 名称(参数)、toolInfo 非空输出 tool_result 名称(id):内容。
buildHistoryGroupMessage 输出格式改为 JSON 行格式：每条 Message 通过 JsonMapper.MAPPER 序列化为一行独立 JSON 对象（LinkedHashMap 保持字段顺序），结构为 {role, content[, reasoning][, tool_calls:[{name, arguments}]][, tool_info:{name, id}]}，assistant 消息在 reasoning 与 toolCalls 均非空时输出 reasoning，tool 消息输出 tool_info(name=toolName/id=toolCallId)；首行保留 HISTORY_GROUP_PREFIX 提示行「【历史消息组{N}】完整内容如下：」，消除 role 消息与 tool_call/tool_result 同级平铺的歧义，LLM 可天然解析。新增 ChatServiceHistoryGroupJsonTest 覆盖 JSON 行格式严格断言。
toHistoryGroupJson 的 tool_calls 数组元素补充 id 字段：构建 callJson 时 name 之后，若 toolCall.getId() 非空则输出 id（与 tool 消息 tool_info.id/toolCallId 一致），使 LLM 可依据 tool_call_id 关联工具调用与结果。
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
AgentContextMutator 在 7 个操作点通过注入的 MessageSender 发送消息：addHistoryEntry() 发送 HistoryMessage；putSessionVariable()/removeSessionVariable() 发送 VariableMessage(scope="SESSION")；putConversationVariable()/removeConversationVariable() 发送 VariableMessage(scope="CONVERSATION")；createChildSession() 发送 ChildCreateSession；sendUserMessage() 发送 ChildMessageEvent；setConversationId() 发送 ConversationIdMessage(sessionId, conversationId)。所有发送操作在 messageSender 为 null 时静默跳过。

### AgentContextManager 消息处理器
AgentContextManager 提供 4 个 public handler 方法供外部系统在收到消息后调用：handleChildCreateSession(ChildCreateSession) 将新子会话添加到父会话缓存的 childSessions 列表中；handleHistoryMessage(HistoryMessage) 将 HistoryEntry 追加到对应会话的 history 列表；handleVariableMessage(VariableMessage) 根据 scope 复制当前变量 Map 并应用 PUT/REMOVE 操作后刷新；handleConversationIdMessage(ConversationIdMessage) 从缓存获取 sessionId 对应的上下文并调用 mutator.setConversationId() 更新对话 ID。

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
新增 sendUserMessageToSession(String sessionId, String content, String modelId, Boolean thinking) 方法：内部使用 SecureRandom 自动生成 24 位 conversationId（字符集 [0-9a-z_]，与 platform-app ConversationController 约定一致），构建 ChatRequest 时通过 .conversationId() 设置，随后复用 processChat 既有逻辑（聊天 + 工具循环）返回最终 assistant Message，用于父会话发起对话时标记对话归属。AgentMessageProxyTest 新增 3 个用例：conversationId 透传与格式校验（24 位 + 字符集匹配 + thinking 透传）、每次调用生成不同 conversationId、工具正常执行后返回文本。
新增 sendUserMessageToSession(String sessionId, String content, String modelId, Boolean thinking) 方法：内部使用 SecureRandom 自动生成 24 位 conversationId（字符集 [0-9a-z_]，与 platform-app ConversationController 约定一致），构建 ChatRequest 时通过 .conversationId() 设置，随后复用 processChat 既有逻辑（聊天 + 工具循环）返回最终 assistant Message，用于父会话发起对话时标记对话归属。AgentMessageProxyTest 新增 3 个用例：conversationId 透传与格式校验（24 位 + 字符集匹配 + thinking 透传）、每次调用生成不同 conversationId、工具正常执行后返回文本。

新增 private ChatDataCacheManager chatDataCacheManager 字段与 public setChatDataCacheManager(ChatDataCacheManager) setter 方法（同包 service.agent 下无需 import），供外部注入聊天数据缓存管理器。
流数据缓存逻辑：processChat 在 collectList 获取 events 后，若 chatDataCacheManager 非 null，调用 startCache(sessionId, conversationId) 创建缓存并遍历 events 通过 cacheEvents 追加块（跳过 finishReason 非 null 的结束块），将 cacheId 透传给 processToolCalls；processToolCalls 新增 String cacheId 参数，工具执行完成（execResult 非 empty）后通过 buildToolResultChunk 构建工具执行结果块（delta 含工具名与执行结果摘要，摘要优先取 getToolStatus 结果，异常回退 execResult.message）追加到缓存，continueAfterTools 流同样 cacheEvents 缓存块并跳过结束块，递归调用透传 cacheId；processChat 返回 Message 前若 chatDataCacheManager 非 null 追加 finishReason=STOP 的结束块。sendUserMessage 未设置 conversationId 时 startCache 第二参传 null，由 provider 实现方决定 key 处理。
流数据缓存逻辑重构：generateConversationId 改为返回时间戳字符串（String.valueOf(System.currentTimeMillis())）。processChat 开头若 request.getConversationId() 为空则生成时间戳并 setConversationId 到 request；流式处理分支：无 chatDataCacheManager 时保持 flux.collectList().block() 原逻辑，有 chatDataCacheManager 时 startCache 创建缓存后改用 flux.doOnNext(event -> 提取 chunk，finishReason 为 null 则 appendChunk).collectList().block() 实现流式追加缓存的同时阻塞获取完整列表。waitForToolCompletion 增加 cacheId 参数，轮询等待期间每次 sleep 前追加空 ChatChunk（delta 为空）到缓存，让流消费端感知工具执行进行中。buildToolResultChunk 仿照 ToolExecutionService.messageSave() 逻辑构建结构化 delta：Map 含 toolName/arguments/result 三键，经 JsonMapper.MAPPER.writeValueAsString 序列化为 JSON 字符串（与 messageSave 的 toolResult 格式一致），序列化失败时回退为工具名与结果摘要拼接。
buildToolResultChunk 补充 toolId：构建的 JSON delta 增加 toolId 字段，格式为 {"toolName":"...","toolId":"...","arguments":"...","result":"..."}，与 ToolExecutionService.messageSave 的 toolInfo（toolCallId/toolName）和 toolResult（toolName/arguments/result）保持一致。
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

MessageName 常量类，定义 6 种消息类型名称：HISTORY_MESSAGE、SESSION_VARIABLE、CONVERSATION_VARIABLE、CHILD_SESSION、CHILD_MESSAGE、CONVERSATION_ID。CONVERSATION_ID 供 ConversationIdMessage 使用，标识会话对话 ID 变更消息。
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

- ChatChunk 与 ChatResponse 的 finishReason 字段类型由 String 改为 FinishReason 枚举（com.ghost616.agentbase.enums.FinishReason）

## 模型请求类型

RequestType 枚举（com.ghost616.agentbase.enums.RequestType），定义模型请求分发方式。包含 RESPONSES("responses", "Responses（有状态）")、RESPONSES_STATELESS("responses_stateless", "Responses（无状态）")、COMPLETIONS("completions", "Chat Completions") 三个值，提供 getCode()/getDescription() 方法及静态 isResponses(String code) 判断（仅对 responses 与 responses_stateless 返回 true，排除 completions）。ModelConfigData.isResponsesType() 委托该方法。
## 会话数据访问层

会话数据访问层已于 2026-07-31 重构：SessionDataProvider 接口被删除，会话级 lastResponseId 的持久化与查询职责迁移至 ContextDataProvider（新增 updateLastResponseId 方法 + AgentContextData.lastResponseId 字段）。lastResponseId 由 AgentContextManager.doBuild 从 loadAgentContext 读取注入上下文（AgentContextMutator.setLastResponseId），流式过程中由 ChatService.toSseStream 通过 contextMutator.setLastResponseId 捕获 chunk.responseId，消息保存完成后由 MessageSavePostHook 通过 contextDataProvider.updateLastResponseId 写回持久层。原 SessionDataProvider 三个方法（updateLastResponseId/getLastResponseId/updateSessionThinking）已随接口删除。
## 会话数据访问层

SessionDataProvider（com.ghost616.agentbase.service.agent.SessionDataProvider），会话数据提供者接口，定义会话级数据的持久化与查询契约，供 SessionManager 懒加载引用，与平台具体数据访问层（platform-app 的 DefaultSessionDataProvider）解耦。包含三个方法：
- updateLastResponseId(String sessionId, String lastResponseId)：更新会话最近一次模型响应 ID（Responses API 有状态续接时作为 previousResponseId）
- getLastResponseId(String sessionId)：查询会话最近一次响应 ID，无则返回 null
- updateSessionThinking(String sessionId, Boolean thinking)：更新会话思考模式开关
## 内置工具执行

## 内置工具执行

支持模型侧内置工具（如 $web_search 等以 $ 前缀标记的工具名）的透传执行。BuiltinToolInvoker（com.ghost616.agentbase.service.agent.invoker）实现 ToolInvoker 接口，execute(ctx, arguments) 直接返回 arguments 字符串，不做实际执行逻辑。

ToolExecutionService.executeTool 在 invoker==null 分支新增判断：当 peekToolCallName 以 "$" 开头时，使用 new BuiltinToolInvoker() 作为调用器，不再返回"工具调用器不存在"错误。
## 工具调用信息链路

工具调用标识统一由 ToolInfo record（com.ghost616.agentbase.dto.model.ToolInfo，字段 toolCallId/toolName）承载，取代原先的单一 String toolCallId 字段。涉及位置：
- dto.model.Message：删除 String toolCallId 字段，新增 ToolInfo toolInfo（tool 角色消息回传用）
- AgentExecutionContext.HistoryEntry：第 4 参由 String toolCallId 改为 ToolInfo toolInfo
- MessageDataProvider.saveMessage 签名与 MessageDTO record：String toolCallId 改为 ToolInfo toolInfo
- SessionManager.MessageSaveBuilder：toolCallId(String) 链式方法改为 toolInfo(ToolInfo)，save() 透传
- ChatService.buildMessageFromEntry：从 entry.toolInfo() 构建 Message.toolInfo
- ToolExecutionService.continueAfterTools：保存 tool 角色消息与构造 HistoryEntry 时使用 new ToolInfo(r.toolId(), r.toolName())
- AgentContextManager.convertMessagesToHistory：直接透传 msg.toolInfo()
- ContextSerializer：HistoryEntry 序列化时将 ToolInfo 输出为含 toolCallId/toolName 的 toolInfo 对象节点
- 桥接脚本：_runner.ts HistoryEntry 接口 toolInfo?: {toolCallId, toolName}；_runner.py HistoryEntry 由 tool_call_id 改为 self.tool_info = data.get("toolInfo")

测试：agent-base 编译通过（test-compile BUILD SUCCESS），全量单测 219/219 通过，_runner.py/_runner.ts 语法校验通过。跨模块影响：agent-integration 的 OpenAIInvoker/AnthropicInvoker/OpenAIResponsesInvoker 使用 Message.getToolCallId()，platform-app 的 DefaultMessageDataProvider/AgentContextController/SessionController 引用旧签名，需各自模块同步。
## 向量化调用
向量化请求 DTO（com.ghost616.agentbase.dto.model.EmbeddingRequest）：model（模型标识）+ input（待向量化的单个文本 String）+ inputList（待向量化的文本列表 List<String>），单条与批量分别由两个字段承载，不再使用 Object 混合类型。
## 向量化调用

ModelInvoker 接口新增 default 方法 EmbeddingResponse embed(EmbeddingRequest request)，用于同步调用模型生成文本向量，默认实现抛出 UnsupportedOperationException（不支持向量化的提供方无需实现），实现类可按需覆写。

向量化请求 DTO（com.ghost616.agentbase.dto.model.EmbeddingRequest）：model（模型标识）+ input（待向量化文本，支持 String 或 List<String>，字段类型为 Object）。向量化响应 DTO（com.ghost616.agentbase.dto.model.EmbeddingResponse）：embeddings（List<EmbeddingItem>，内部类 EmbeddingItem 含 index + List<Float> embedding 浮点数组）+ usage（UsageInfo Token 用量）。均使用 Lombok @Data @Builder @NoArgsConstructor @AllArgsConstructor。
## 模型类型

ModelType 枚举（com.ghost616.agentbase.enums.ModelType），定义模型的能力类型，包含 LLM("LLM", "大语言模型") 与 EMBEDDINGS("EMBEDDINGS", "向量嵌入模型") 两个值。code 字段使用 @EnumValue 标注，提供 getCode()/getDescription() 方法。供 ModelConfigData.modelType 字段引用，用于区分对话/向量化模型。
## ConversationIdMessage

ConversationIdMessage 消息类，继承 SessionMessage，messageName=CONVERSATION_ID。携带 String conversationId 字段（对应会话归属的对话 ID）。由 AgentContextMutator.setConversationId() 在设置对话 ID 时触发发送，供外部系统收到消息后通过 AgentContextManager.handleConversationIdMessage() 同步更新缓存上下文。
## 智能体日志接口框架

ChatService 新增私有方法 addLog(LogData logData)：从 registry.getAgentLog() 获取 AgentLog，非 null 时调用 agentLog.addLog(logData)，调用以 try-catch 包裹，捕获异常时以 WARN 级别记录日志（含异常堆栈），确保 AgentLog 实现抛异常不会中断 chat() 主流程。chat() 入口中：context 构建后、isToolContinue 判断后，一次 addLog 调用以 RequestEntryLogData.builder() 构建完整日志（.logLevel(LogLevel.INFO)、context、sessionId、modelId=context.getModelId()、content、isToolContinue）并记录，不再拆分多次调用。
智能体日志类型扩展：LogType 枚举新增 ERROR_LOG（错误日志）/ROUTE（路由分发）/MODEL_CALL（模型调用）/STREAM_EVENT（流式事件）/HISTORY_EXPAND（历史展开）/SKILL_LOAD（技能加载）6 个值；LogLevel 新增 WARN（警告）级别，支持错误日志以警告级别记录。

新增 6 个 LogData 子类（均继承 ContextLogData，@SuperBuilder，仅 getter，无公开构造器）：
- ErrorLogData：errorCode(String)/message(String)/exception(Throwable)，logType()=ERROR_LOG
- RouteLogData：requestType(String)，logType()=ROUTE
- ModelCallLogData：messageCount(int)/toolCount(int)/thinking(Boolean)，logType()=MODEL_CALL
- StreamEventLogData：eventType(String)/hasToolCalls(Boolean)，logType()=STREAM_EVENT
- HistoryExpandLogData：foldedCount(int)/expandedIndices(List<Integer>)，logType()=HISTORY_EXPAND
- SkillLoadLogData：skillNames(List<String>)/skillCount(int)，logType()=SKILL_LOAD

ChatService 新增 11 处 addLog 调用：
- ERROR 级 ErrorLogData：conversationId 为空抛异常前（errorCode=PARAM_INVALID）；modelId 无配置抛异常前（errorCode=MODEL_NOT_FOUND）；parseLoadedSkills JSON 解析异常（SYSTEM_ERROR+exception）；parseExpandedIndices JSON 解析异常（SYSTEM_ERROR+exception，该方法签名新增 context 参数）；buildHistoryGroupMessage 序列化失败（WARN 级，SYSTEM_ERROR，该方法签名新增 context 参数）
- INFO 级 RouteLogData：请求路由后记录 requestType
- INFO 级 ModelCallLogData：chatViaChatCompletions 模型调用前记录 messageCount/toolCount/thinking
- INFO 级 StreamEventLogData：toSseStream 首次检测到 tool calls 时记录（eventType=ToolCallDetected，compareAndSet 保证仅记录一次）；finishReason 非空时记录（StreamComplete+hasToolCalls）；doOnCancel 时记录（StreamCancelled）
- INFO 级 HistoryExpandLogData：foldMessageGroups 实际折叠时记录 foldedCount+expandedIndices
- INFO 级 SkillLoadLogData：buildContextSystemInfo 已加载技能非空时记录 skillNames+skillCount
日志数据字段调整：HistoryExpandLogData 的 expandedIndices(List<Integer>) 字段改为 expandedMessages(List<String>)，记录展开的历史消息组锚点消息内容（由 buildHistoryGroupMessage 返回的 Message.content 收集），foldMessageGroups 中折叠日志 addLog 调用调整到锚点消息构建之后，收集 anchorMessages 的 content 列表作为 expandedMessages；ModelCallLogData 新增 toolNames(List<String>) 字段，记录发给模型的工具名称列表，chatViaChatCompletions 中模型调用日志 addLog 调用通过 tools.stream().map(ToolDefinition::getName) 收集传入。
AgentContextManager 新增私有方法 addLog(LogData logData)：与 ChatService 一致，从 registry.getAgentLog() 获取 AgentLog，非 null 时调用 agentLog.addLog(logData)，调用以 try-catch 包裹，捕获异常时以 WARN 级别记录日志，确保日志实现异常不中断上下文管理主流程。

智能体日志类型扩展：LogType 枚举新增 CONTEXT_BUILD（上下文构建）/CHILD_SESSION（子会话创建）/REFRESH（上下文刷新）/HANDLE_MESSAGE（消息处理）/CACHE_REMOVE（缓存移除）5 个值。

新增 5 个 LogData 子类（均继承 ContextLogData，@SuperBuilder，仅 getter，无公开构造器）：
- ContextBuildLogData：sessionId/agentId/modelId/toolCount(int)/historyCount(int)/isSubSession(boolean)/cacheHit(boolean)，logType()=CONTEXT_BUILD
- ChildSessionLogData：parentSessionId/childSessionId/sessionName，logType()=CHILD_SESSION
- RefreshLogData：sessionId/refreshTarget(String: HISTORY/SESSION_VARIABLES/CONVERSATION_VARIABLES/CHILD_SESSIONS)，logType()=REFRESH
- HandleMessageLogData：sessionId/messageType(String: CHILD_CREATE_SESSION/HISTORY_MESSAGE/VARIABLE_MESSAGE/CONVERSATION_ID)，logType()=HANDLE_MESSAGE
- CacheRemoveLogData：sessionId，logType()=CACHE_REMOVE

AgentContextManager 新增 12 处 addLog 调用：
- doBuild()：INFO 级 ContextBuildLogData 记录 sessionId/agentId/modelId/toolCount/historyCount/isSubSession/cacheHit(false)；会话未找到（ctxData 为 null）抛异常前记录 ERROR 级 ErrorLogData（errorCode=SESSION_NOT_FOUND）
- createChildSession()：INFO 级 ChildSessionLogData 记录 parentSessionId/childSessionId/sessionName
- refreshHistory/refreshSessionVariables/refreshConversationVariables/refreshChildSessions：INFO 级 RefreshLogData 记录 sessionId 与对应 refreshTarget（HISTORY/SESSION_VARIABLES/CONVERSATION_VARIABLES/CHILD_SESSIONS），仅缓存中存在上下文时记录
- handleChildCreateSession/handleHistoryMessage/handleVariableMessage/handleConversationIdMessage：INFO 级 HandleMessageLogData 记录 sessionId 与对应 messageType（CHILD_CREATE_SESSION/HISTORY_MESSAGE/VARIABLE_MESSAGE/CONVERSATION_ID），仅缓存中存在上下文时记录
- remove()：INFO 级 CacheRemoveLogData 记录 sessionId
智能体日志类型扩展：LogType 枚举新增 MESSAGE_SAVE（消息保存）/MESSAGE_QUERY（消息查询）/MESSAGE_ROLLBACK（消息回退）/TOOL_EXECUTE（工具执行）/TOOL_CONTINUE（工具执行后继续）5 个值。

新增 5 个 LogData 子类（均继承 ContextLogData，@SuperBuilder，仅 getter，无公开构造器）：
- MessageSaveLogData：sessionId/role/contentLength(int)，logType()=MESSAGE_SAVE
- MessageQueryLogData：sessionId/messageCount(int)，logType()=MESSAGE_QUERY
- MessageRollbackLogData：sessionId/rollbackCount(int)，logType()=MESSAGE_ROLLBACK
- ToolExecuteLogData：sessionId/toolCallId/toolCallName/toolCallArguments/toolType(String: system/regular/builtin)/queueStatus(String: empty/executing/failed/error)，logType()=TOOL_EXECUTE
- ToolContinueLogData：sessionId/resultCount(int)/toolNames(List<String>)，logType()=TOOL_CONTINUE

SessionManager 新增私有 addLog 方法（与 ChatService 一致）：save() 校验失败（sessionId/role/content 任一为 null）时记录 ERROR 级 ErrorLogData（errorCode=PARAM_INVALID），保存成功后记录 INFO 级 MessageSaveLogData（sessionId/role/contentLength）；getMessages() 后记录 INFO 级 MessageQueryLogData（messageCount）；rollbackToLastUserMessage() 后记录 INFO 级 MessageRollbackLogData（rollbackCount）。

ToolExecutionService 新增私有 addLog 方法与 resolveToolType 辅助方法（_sys_ 前缀→system、$ 前缀→builtin、其他→regular）。executeTool 各分支记录 ToolExecuteLogData：队列为空记录 INFO empty、获取调用器异常记录 ERROR failed、invoker 不存在（非 $ 前缀）记录 ERROR error、poll 后 toolCall 为 null 记录 INFO empty、会话上下文不存在记录 ERROR error、context 停止记录 INFO empty、正常流程记录 INFO executing（同步启动与异步完成各一处）、异步执行异常记录 ERROR failed；continueAfterTools() 记录 INFO 级 ToolContinueLogData（resultCount/toolNames）。
MessageSaveLogData 字段调整：去掉 contentLength(int)，新增 messageId(String)/content(String)/reasoning(String)/toolInfo(ToolInfo)/toolResult(String)/toolCalls(List&lt;MessageDataProvider.ToolCallData&gt;)/usage(UsageInfo)/webSearchCall(List&lt;MessageDataProvider.WebSearchCallData&gt;)/customToolCall(List&lt;MessageDataProvider.CustomToolCallData&gt;)/conversationId(String)。SessionManager.MessageSaveBuilder.save() 在 MessageSaveLogData build 调用中传入 save() 返回的 messageId 及 builder 全部字段。
RequestEntryLogData 新增 conversationId(String) 字段：chat() 入口在构建日志前从 request.getConversationId() 提前提取 conversationId（先于 addLog 调用），通过 .conversationId() 设置到 RequestEntryLogData.builder()，避免原"先打日志后提取"的时序问题导致请求入口日志缺失对话 ID。
LogData 类层次重构：新增 SessionLogData 抽象类（extends LogData，@SuperBuilder），字段 sessionId(String)/conversationId(String)，用于承载会话级信息；原继承 ContextLogData 的 6 个类改为继承 SessionLogData：MessageSaveLogData（移除自有 sessionId/conversationId 字段，改由父类承载）、MessageQueryLogData、MessageRollbackLogData、CacheRemoveLogData（均移除自有 sessionId 字段）、ChildSessionLogData（原 parentSessionId 字段移除，对应父类 sessionId）、SendMessageLogData（原直接继承 LogData + parentSessionId 字段，改为继承 SessionLogData，parentSessionId 对应 sessionId）。AgentContextManager 构建 ChildSessionLogData/SendMessageLogData 时改用 .sessionId(parentSessionId)。
新增 SessionErrorLogData 类（extends SessionLogData），字段 errorCode(String)/message(String)/exception(Throwable)，logType()=ERROR_LOG，用于无 AgentExecutionContext 时替代 ErrorLogData 记录会话级错误。SessionManager.MessageSaveBuilder.save() 三处参数校验失败（sessionId/role/content 为 null）错误日志与 AgentContextManager.doBuild() 会话未找到错误日志由 ErrorLogData 改用 SessionErrorLogData（携带 sessionId/conversationId）。
ToolExecutionService.executeTool() 重排：agentContextManager.get(sessionId) 及 null 校验前置到 peek 之前（返回 error 时不再携带工具字段），context 在方法开头获取，后续所有 ToolExecuteLogData 调用均携带 .context(context)。
AgentContextManager.injectVariableCallbacks() 在 parentCtx != null（构建子会话上下文）时通过 parentCtx.context().getConversationId() 获取父会话 conversationId，并将其传入 createChildSession/sendUserMessage 回调；createChildSession()/sendUserMessage() 私有方法签名新增 conversationId 参数，构建 ChildSessionLogData/SendMessageLogData 时通过 .conversationId(conversationId) 携带父会话对话 ID。主会话（parentCtx == null）时 conversationId 为 null，日志 conversationId 保持 null。
RequestEntryLogData 移除 conversationId 字段：RequestEntryLogData 构建从 chat() 方法头部（isToolContinue 判断前）移至 isToolContinue 代码块之后（此时 contextMutator.setConversationId() 已执行），context 已携带 conversationId（经 context.getConversationId() 读取），不再通过 .conversationId() 额外传入。LogType 枚举移除 CALL_SOURCE 值，枚举值由 19 个降为 18 个。
## 聊天数据缓存

聊天数据缓存基础设施（com.ghost616.agentbase.service.agent），用于缓存流式聊天片段并支持按序号区间回溯读取为 SSE 流。

ChatDataCacheProvider 接口定义缓存契约：createCache(sessionId, conversationId) 创建缓存并返回缓存 ID（相同会话与对话已存在时返回 null）、cacheExists(cacheId) 判断缓存是否存在、cacheExists(sessionId, conversationId) 按会话+对话判断缓存是否存在、isCacheDone(cacheId) 判断缓存是否已结束（实现方跟踪状态）、getCacheId(sessionId, conversationId) 按会话+对话返回缓存 ID、getCacheSessionInfo(cacheId) 按缓存 ID 返回 CacheSessionInfo（含 sessionId/conversationId，缓存不存在时返回 null）、getMaxChunkIndex(cacheId) 获取最大块序号（无数据返回 -1）、appendChunk(cacheId, chunk) 追加聊天块、removeCache(cacheId) 删除缓存、getChunks(cacheId, startIndex, endIndex) 读取指定序号范围（含两端）的聊天块列表。

CacheSessionInfo record（com.ghost616.agentbase.service.agent），包含 String sessionId 与 String conversationId 两个字段，承载缓存所属的会话与对话 ID，由 ChatDataCacheProvider.getCacheSessionInfo(cacheId) 返回。

ChatDataCacheManager 通过构造函数注入 ChatDataCacheProvider，提供五个方法：getCacheId(sessionId, conversationId) 委托 provider 按会话+对话返回缓存 ID；startCache(sessionId, conversationId) 先调用 provider.cacheExists(sessionId, conversationId) 判断缓存是否已存在，存在则抛 BusinessException(DUPLICATE_KEY)，否则调用 provider.createCache 创建缓存并返回缓存 ID（返回 null 时同样抛 DUPLICATE_KEY）；appendChunk(cacheId, chunk) 校验 provider.cacheExists(cacheId) 不存在抛 NOT_FOUND、provider.isCacheDone(cacheId) 已结束抛 PARAM_INVALID，校验通过后委托 provider 追加聊天块；removeCache(cacheId) 委托 provider 删除缓存；getStream(cacheId, startIndex) 校验缓存存在后，从 getChunks 读取块逐个输出并检查 finishReason，若所有可用块均未遇到 finishReason 非 null 的块，则用 Flux.interval(1ms) 轮询 getChunks 获取新增块逐块输出并检查 finishReason，仅通过块中 finishReason 判定流结束（遇到 finishReason 非 null 的块则终止），5 分钟内无新数据时生成 finishReason=ERROR 结束块并终止流；缓存不存在抛 NOT_FOUND、缓存无数据（getMaxChunkIndex 返回 -1）抛 BusinessException(NOT_FOUND, "缓存无数据")、startIndex 大于最大序号（maxIndex >= 0 时）抛 PARAM_INVALID；缓存存在但暂无数据时不进入轮询，直接抛出异常。

智能体日志能力（LogType 新增 CHAT_CACHE("CHAT_CACHE", "对话数据缓存")）：新建 ChatCacheLogData（com.ghost616.agentbase.service.agent.log，继承 SessionLogData，@SuperBuilder，含 cacheId 与 operation 字段，logType()=CHAT_CACHE）。ChatDataCacheManager 新增 private AgentLog agentLog 字段与 setAgentLog 注入方法、addLog 私有方法（agentLog 非 null 时以 try-catch 调用 addLog，异常不中断主流程）及 addCacheLog 辅助方法，在关键节点记录 ChatCacheLogData（logLevel INFO/ERROR，operation 为 CACHE_START/CACHE_APPEND/CACHE_REMOVE/CACHE_STREAM）：startCache 各日志 operation=CACHE_START；appendChunk 校验失败 ERROR 日志 operation=CACHE_APPEND，成功日志仅首个块（chunk.index==0 或 provider.getMaxChunkIndex==0）与结束块（chunk.finishReason!=null）时记录；removeCache operation=CACHE_REMOVE；getStream 起止（doFinally）operation=CACHE_STREAM。仅持有 cacheId 时通过 provider.getCacheSessionInfo(cacheId) 返回的 CacheSessionInfo 解析会话/对话 ID 构造日志。
## 模型响应结束原因

FinishReason 枚举（com.ghost616.agentbase.enums.FinishReason），定义模型响应的结束原因。包含 STOP("stop", "正常结束")、LENGTH("length", "达到长度限制")、TOOL_CALLS("tool_calls", "触发工具调用")、CONTENT_FILTER("content_filter", "内容被过滤")、ERROR("error", "发生错误")、CANCELLED("cancelled", "被取消") 六个值。code 字段使用 @EnumValue 标注，提供 getCode()/getDescription() 方法及静态 fromCode(String code) 转换（未知/为 null 时返回 null）。供 ChatChunk/ChatResponse 的 finishReason 字段引用。
- FinishReason 枚举的 getCode() 方法标注 @JsonValue 注解（com.fasterxml.jackson.annotation.JsonValue），使枚举序列化为小写 code（如 "stop"/"error"）而非大写枚举名，保持与前端 SSE 消费端 chunk.finishReason === 'stop'/'error' 协议一致；反序列化仍走 fromCode 或依赖 JSON 值匹配（@JsonValue 同时影响序列化与反序列化）
