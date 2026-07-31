# platform-app 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## 模块功能说明

platform-app 模块包含以下功能：
1. 智能体(Agent)上下文管理 - ContextDataProvider、MessageDataProvider 等实现
2. 模型调用器(Model Invoker) - 支持 OpenAI、Anthropic、Azure、Ollama、DeepSeek、Custom 等平台
3. Agent 上下文配置 (AgentContextConfiguration) - Spring Bean 装配
4. 所有 agent-base 依赖通过子包路径导入（如 com.ghost616.agentbase.dto.*、com.ghost616.agentbase.enums.*、com.ghost616.agentbase.service.* 等）
- 新增 MessageDataProvider、ContextDataProvider、ModelInvokerDataProvider、SystemTool 等 agent-base 接口的跨包 import 修复
- ChatService 中 HookInvoker/SystemHook/SystemPostHook 的 import 已从 com.ghost616.platform.service.hook 迁移至 com.ghost616.agentbase.service.agent.invoker
- AgentContextConfiguration 新增 MessageSavePostHook @Bean，自动注入 SessionManager、AgentContextManager、ToolCallQueueManager
- ToolExecutionController 中 ToolExecutionTracker 的 import 已从 platform.service.agent 迁移至 com.ghost616.agentbase.service.agent
- DefaultModelInvokerFactory（实现 ModelInvokerFactory）已创建，注入 RestClient.Builder 和 WebClient.Builder，根据 platformType 创建对应 Invoker
- ModelInvokerManager import 已从 platform.service.model.invoker 迁移至 com.ghost616.agentbase.service.model.invoker
- getInvoker 调用方式改为：构建 ModelConfigData 后传入 modelInvokerManager.getInvoker(ModelConfigData)
- AgentContextConfiguration 新增 @Bean defaultModelInvokerFactory 和 @Bean modelInvokerManager
- 已删除过期注释文件 ModelInvokerConfiguration.java
5. 默认聊天数据提供者(DefaultChatDataProvider) - 实现 ChatDataProvider 接口，通过 ModelConfigMapper/SessionMapper 执行 DB 操作，通过 ApplicationContext 获取 HookInvoker
- AgentContextConfiguration 新增 defaultChatDataProvider @Bean 和 chatService @Bean，chatService 创建后调用 initHooks() 初始化钩子
- ChatController 中 ChatRequest 和 ChatService 的 import 已迁移至 agent-base 包
- ToolExecutionController 已重构：业务逻辑委托给 agent-base 的 ToolExecutionService，controller 仅保留 API 端点方法
- AgentContextConfiguration 新增 @Bean toolExecutionTracker 和 @Bean toolExecutionService
- AgentContextConfigurationTest 修复：移除 chatService_创建并调用initHooks 测试方法中多余的 ObjectMapper 参数，对齐 AgentContextConfiguration.chatService() 方法签名
- 模型调用器实现已全部迁移至 agent-integration 模块（OpenAIInvoker、AnthropicInvoker、AzureInvoker、OllamaInvoker、DeepSeekInvoker、CustomInvoker）
- 新增 agent-integration 依赖，使用其 Build 类管理组件装配
- AgentContextConfiguration 重构：仅暴露 SystemToolProvider、系统工具（HistoryQuerySystemTool/LoadSkillsSystemTool/UnloadSkillsSystemTool）、MessageSavePostHook、DefaultChatDataProvider、ModelInvokerFactory、Build、ChatService、ToolExecutionService 的 @Bean，其余组件由 Build 内部创建
- DefaultModelInvokerFactory 的实现移至 agent-integration 模块
- DefaultModelInvokerDataProvider 已废弃并删除
- AgentContextConfiguration 新增 @Bean sessionManager(Build)、@Bean agentContextManager(Build)、@Bean toolManager(Build)、@Bean modelInvokerManager(Build)，这些组件通过 Build 类内部创建后暴露为 Spring Bean
- MessageSavePostHook @Bean 改用注入的 SessionManager/AgentContextManager 参数 + Build.toolCallQueueManager()，不再直接调用 Build getter
- chatService() @Bean 添加 @DependsOn("messageSavePostHook") 确保 MessageSavePostHook 先创建
- 已移除 messageSavePostHook() @Bean（该 hook 现由 AgentAssembler 的 ChatDataProvider 代理内部管理）
- 已移除 @DependsOn("messageSavePostHook") 注解及相关 import
- 已移除 historyQuerySystemTool()、loadSkillsSystemTool()、unloadSkillsSystemTool() @Bean 及相关 import（这三个系统工具现由 AgentAssembler 的 SystemToolProvider 代理内部管理）
- DefaultContextDataProvider 新增 createChildSession 空实现，返回 null
- Session 实体新增 parentSessionId、isChild、description 字段（子会话支持）
- 新增 SessionSkill 实体和 SessionSkillMapper（技能关联表）
- DefaultContextDataProvider.createChildSession 实现：校验父会话/模型/工具/技能存在性，创建子会话并写入 SessionTool/SessionSkill 关联记录
- DefaultContextDataProvider.loadAgentContext 子会话分支填充 parentSessionId=session.getParentSessionId()、childSessions=null
- DefaultContextDataProvider.loadAgentContext 普通会话分支填充 parentSessionId=null、childSessions 从数据库查询 parentSessionId=当前会话ID 的子会话列表
- DefaultContextDataProvider.createChildSession 第二个参数名从 agentName 改为 sessionName（仅 Java 参数名变更，不影响接口契约）
- 新增 SystemTestSubSessionTool（系统测试工具），位于 com.ghost616.platform.systemtest 包，实现 SystemTool 接口，工具名 system_test，功能：创建子会话并发送消息获取回复
- SessionDTO 新增 parentSessionId/isChild/description 字段，toDTO 方法映射新字段
- 新增 SessionService.listChildSessions 方法：根据父会话ID查询所有子会话
- SessionController 新增 GET /api/sessions/{id}/children 端点
- 新增 DefaultSubSessionCallback（实现 SubSessionCallback，@Component），execute 方法返回 null
- 新增 SubSessionCallbackSystemTool（实现 SystemTool），包装 SubSessionCallback，工具名 sub_session_callback
- AgentContextConfiguration.systemToolProvider 注入 DefaultSubSessionCallback，在 tools Map 中添加 SubSessionCallbackSystemTool
6. DefaultSubSessionCallback 核心逻辑实现：
   - 注入 ContextDataProvider 依赖，通过子会话 ID 获取父会话 ID
   - SubSessionData 内部类（childSessionId、userMessage、CompletableFuture<Message>）
   - ConcurrentHashMap<Long, SubSessionData> 以 parentSessionId 为键管理子会话数据
   - execute 方法：通过 loadAgentContext 获取 parentSessionId，创建 CompletableFuture 阻塞等待，完成后清理 map 条目
   - getSubSessionData(Long parentSessionId) 公共方法：通过父会话 ID 获取数据对象
- 已删除 SpawnSubAgentSystemTool（子智能体生成系统工具），因其功能由 agent-engine 模块的智能体编排能力替代
6. 新增 ToolStatusResultDTO 数据传输对象，封装工具执行结果的全部字段（status/toolId/toolName/arguments/hasMore/result/message）+ needsSubSessionFlow 布尔字段
7. ToolExecutionController 中 executeTools 和 toolStatus 两个接口的返回类型统一改为 ApiResponse<ToolStatusResultDTO>，并在检测到工具名为 _sys_callback_sub_session 时通过 DefaultSubSessionCallback.getSubSessionData 判断是否有待处理的子会话数据，设置 needsSubSessionFlow=true
- 实体 SessionSkill/AgentSkill/SessionTool/AgentTool 新增 sessionAuth(SessionAuthType) 字段，记录授权范围
- schema.sql 中 session_tool/agent_tool/session_skill/agent_skill/skill_config 表新增 session_auth VARCHAR(32) 列
- SkillConfig 实体新增 sessionAuth 字段
- SkillConfigServiceImpl.toDTO 映射 sessionAuth 字段
- DefaultContextDataProvider.createChildSession 创建 SessionTool 和 SessionSkill 时设置 sessionAuth 值（SessionTool 从 ToolConfigDTO.sessionAuth 获取，SessionSkill 从 SkillConfig.sessionAuth 获取）
- 数据库 Schema 修复：session_tool/agent_tool/agent_skill/session_skill 四张表的 session_auth 列默认值设为 0，SchemaMigration 迁移默认值改为 "0" 并追加幂等 NULL 回填逻辑，解决 SQLite getObject(Integer.class) 遇 NULL 抛 Bad value 异常问题
- Session 实体新增 thinking (Boolean) 字段，标识子会话是否启用思考模式
- DefaultContextDataProvider.createChildSession 新增 Boolean thinking 参数，创建 Session 时设置 thinking 字段
- SystemTestSubSessionTool 从 JSON 参数中读取 thinking，传递给 createChildSession
- SystemTestSubSessionTool 从 JSON 参数中读取 thinking 字段传递给 sendUserMessage（不再传给 createChildSession）
- Session 表新增 thinking TINYINT(1) 列，SchemaMigration 增量迁移支持
- SchemaMigration 新增 session.thinking 列的 ALTER TABLE 迁移
- DefaultSubSessionCallback.execute 方法新增第三个参数 Boolean thinking，与 SubSessionCallback 接口签名一致（透传接收）
- DefaultSubSessionCallback.SubSessionData 内部类新增 thinking(Boolean) 字段、构造参数及 getter；execute 方法创建 SubSessionData 时传递 thinking 参数
- SubSessionDataDTO 新增 thinking 字段，SessionController.getSubSessionData() 映射 thinking 到 DTO
- 新增 SubSessionDataDTOTest（15 用例覆盖 thinking 序列化/反序列化/构造器/equals/hashCode）
- 新增 SessionControllerTest（4 用例覆盖 getSubSessionData 端点 thinking 映射）
- DefaultMessageDataProvider.saveMessage 方法签名新增 UsageInfo usage 参数，满足 MessageDataProvider 接口契约；方法体内忽略 usage 不持久化
- DefaultContextDataProvider 实现 ContextDataProvider 新增 4 个方法：getLatestMessages（委托 MessageDataProvider）、getLatestSessionVariables（复用 loadSessionVariablesInternal）、getLatestConversationVariables（复用 loadSessionVariablesInternal）、getLatestChildSessions（查询 Session 表 parentSessionId）
- DefaultContextDataProvider.getLatestConversationVariables 改为返回 Map.of()，后续从 Redis 缓存获取
- AgentContextConfiguration.agentAssembler() 构造函数第 7 个参数传入 null（MessageSender），添加 import com.ghost616.agentbase.sendmessage.MessageSender
5. 新增 DefaultToolExecutionProvider（实现 ToolExecutionProvider 接口）：
   - 持有三个 ConcurrentHashMap 数据容器（toolCallQueues/currentExecutions/completedResults）
   - 实现工具调用队列操作：enqueue/poll/peek/hasPending/clearQueue
   - 实现工具执行状态追踪：updateExecution/clearTracking/getCurrentExecution/getAndClearResults
- PlatformType 枚举新增 KIMI（Kimi 月之暗面，https://api.moonshot.cn/v1，模型：kimi-k2.7-code/kimi-k2.6/kimi-k2.5/kimi-k3）和 VOLCENGINE（火山引擎，https://ark.cn-beijing.volces.com/api/v3，模型：doubao-seed-evolving/doubao-seed-2-1-turbo-260628/doubao-seed-2-1-pro-260628）

- 会话回滚改为软回滚：message 表新增 rollback TINYINT NOT NULL DEFAULT 0 字段，回滚操作不再物理删除消息、不再修改已消耗 token 数，改为设置 rollback=1 标记，getMessages 查询过滤掉 rollback=1 的消息
- chatService() @Bean 中已移除 refreshHooks() 调用，HookManager.refreshHooks 由 AgentAssembler.build() 统一管理
- DefaultChatDataProvider.getHooks(Long sessionId) 已实现，返回空列表（List.of()），作为 ChatDataProvider 接口带参 getHooks 的重载实现
- DefaultToolDataProvider 实现 ToolDataProvider 新增的 getCustomInvoker 方法（抛出 UnsupportedOperationException）
- 已删除 DefaultCustomToolInvokerProvider（原实现 CustomToolInvokerProvider），AgentContextConfiguration 中移除相关 @Bean、import 及 agentAssembler 参数，AgentContextConfigurationTest 同步清理
## ID 类型转换层

- IdConverter 工具类（com.ghost616.platform.util.IdConverter）提供 parse/toString/parseList/toStringList 方法，统一处理 agent-base（String）与 platform-app（Long）之间的 ID 类型转换
- DataProvider 实现类（DefaultContextDataProvider、DefaultMessageDataProvider、DefaultChatDataProvider、DefaultToolDataProvider、DefaultToolExecutionProvider）的接口方法入参 String ID 转换为 Long 后再执行内部逻辑，返回值中的 Long ID 转换为 String
- DefaultSubSessionCallback 的 SubSessionCallback.execute 方法签名适配为 String sessionId，内部转换为 Long
- Controller 层（ChatController、ToolExecutionController、AgentContextController）中调用 agent-base 服务时，将 Controller 路径参数 Long 转换为 String 传入
- SessionServiceImpl 中调用 SessionManager、AgentContextManager、ToolManager 时，将 Long sessionId 转换为 String
- AgentContextController 中从 AgentExecutionContext（String ID）读取数据填充 AgentContextDTO（Long ID）时，将 String 转换为 Long
## 评估管理

- 智能体评估(AgentEvaluation) CRUD 接口：DTO（AgentEvaluationDTO/AgentEvaluationCreateRequest/AgentEvaluationUpdateRequest）、Service（AgentEvaluationService 接口与 AgentEvaluationServiceImpl 实现）、Controller（AgentEvaluationController，路径 /api/agent-evaluations）
- 支持 name 唯一性校验（create 和 update 时检查，重复抛 AGENT_EVALUATION_ALREADY_EXISTS）
- 级联删除：删除 AgentEvaluation 时同时删除关联的 Evaluation、EvaluationResult、benchmark Session、execution Session 及关联数据（SessionVariable、SessionTool、SessionSkill、Message、MessageToolCall）
- 评估(Evaluation) CRUD 接口：DTO（EvaluationDTO/EvaluationCreateRequest/EvaluationUpdateRequest）、Service（EvaluationService 接口与 EvaluationServiceImpl 实现）、Controller（EvaluationController，路径 /api/evaluations）
- EvaluationCreateRequest 新增 agentEvalId 字段替代原 benchmarkSessionId；Evaluation.list 和 EvaluationController.list 支持 agentEvalId 查询参数过滤
- Evaluation.create 自动创建基准 Session（agentId 从 AgentEvaluation 获取，modelId 从请求参数获取，title=name+BenchmarkSession，isEvaluation=true，systemPrompt 从 agent 表获取），并将 Session ID 设置为 evaluation 的 benchmarkSessionId
- EvaluationDTO 新增 agentEvalId、agentId、agentName 字段，toDTO 通过 AgentConfigMapper 查询 agentName
- 级联删除：删除 Evaluation 时同时删除关联的 EvaluationResult、benchmark Session 及子会话相关数据（Session、SessionVariable、SessionTool、SessionSkill、Message、MessageToolCall）
- 评估后台执行(EvaluationExecutionService)：接收 evaluationId 执行异步评估，验证基准会话有用户消息，复制基准会话数据创建执行会话，通过 AgentMessageProxy 逐条执行用户消息（含工具调用），完成后调用 EvaluationResultGenerateService 生成评估结果。内存中维护 ConcurrentHashMap 存储执行状态（evaluationId → EvaluationExecutionStatusDTO），提供 getStatus 查询
- 评估结果生成(EvaluationResultGenerateService)：接收基准会话ID和执行会话ID，获取双方全部消息，构建系统提示词对比差异度，调用 evaluation.modelId 对应模型执行评估，将评估结果写入 EvaluationResult 表
- 执行状态 DTO(EvaluationExecutionStatusDTO)：包含 evaluationId/executionSessionId/status/currentStep/totalSteps 五个字段
- 前台创建响应 DTO(EvaluationSessionCreateResponse)：包含 sessionId/userMessages 列表两个字段
- EvaluationResultDTO 新增 executionStatus 字段
- 评估后台执行控制器(EvaluationExecutionController)：POST /api/evaluations/{id}/execute（异步执行）、GET /api/evaluations/{id}/execute/status（状态轮询）
- 评估前台会话控制器(EvaluationSessionController)：POST /api/evaluations/{id}/session（复制基准会话→返回执行会话ID+user消息列表）、POST /api/evaluations/{id}/session/{sessionId}/generate（接收评估ID和会话ID，调用结果生成服务）
- PlatformApplication 添加 @EnableAsync 注解，支持异步任务执行
- 审查修复：asyncExecute 提取到独立 @Component AsyncEvaluationExecutor 解决 @Async 自调用问题；executionStatusMap 增加 TTL(1h) + @Scheduled 清理机制；createExecutionSession 新增 Evaluation 参数重载避免重复查询
- EvaluationResultDTO 新增 modelId(Long, @JsonSerialize ToStringSerializer) 和 finalScore(Integer) 字段
- EvaluationResultGenerateService 生成评估结果后，调用同一模型从评估结果文本中提取最终评分数字（构建 prompt 要求仅返回数字），将提取的 finalScore 和 Evaluation.modelId 写入 EvaluationResult
- EvaluationServiceImpl.toResultDTO 映射 EvaluationResult 的 modelId 和 finalScore 字段到 DTO
- 评估结果删除增强：EvaluationService 新增 batchDeleteResults（@Transactional，循环调用 deleteResult 级联清理 session/message/messageToolCall/sessionVariable/sessionTool/sessionSkill）与 clearResults（按 evaluationId 查询全部 resultId 后批量删除）；EvaluationController 新增 POST /api/evaluations/results/batch-delete（@RequestBody List<Long> 批量删除）和 DELETE /api/evaluations/{evaluationId}/results（清空该评估所有结果）