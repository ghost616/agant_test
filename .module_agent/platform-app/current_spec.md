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
- 重命名重构：EvaluationExecutionStatusDTO 字段 executionSessionId → executionSession；EvaluationExecutionService/EvaluationResultGenerateService 相关参数名同步重命名（类型仍为 Long）；AsyncEvaluationExecutor.executeAsync 参数从 Long executionSessionId 改为 Session executionSession（从 Session 获取 thinking 透传），改用 AgentMessageProxy.sendUserMessageToSession 替代 sendUserMessage
- EvaluationExecutionService.copyBenchmarkSession 在 setSystemPrompt 后新增 setThinking(benchmarkSession.getThinking())，执行会话透传基准会话 thinking 字段，配合 AsyncEvaluationExecutor.executeAsync 从 Session 读取 thinking
- 恢复 executionSessionId 命名：EvaluationExecutionStatusDTO 字段名恢复为 executionSessionId（JSON 序列化键为 executionSessionId）；EvaluationExecutionService 的 generateResult/generateResultAsync/getGenerateStatus 与 EvaluationResultGenerateService.generate 参数名恢复为 executionSessionId（Long）；AsyncEvaluationExecutor.generateResultAsync 参数恢复为 Long executionSessionId，executeAsync 保留 Session executionSession 参数（仅从 Session 取 thinking 透传，改用 AgentMessageProxy.sendUserMessageToSession），内部以 Long executionSessionId 向下传递
- EvaluationExecutionController 新增 GET /api/evaluations/session/{executionSessionId}/stream 端点（produces=text/event-stream，返回 Flux<ServerSentEvent<ChatChunk>>）：注入 ChatDataCacheManager 与 DefaultChatDataCacheProvider，String sessionId=String.valueOf(executionSessionId)，调用 defaultChatDataCacheProvider.getCacheIdsBySessionId(sessionId) 取第一个 cacheId，列表为空返回空 Flux，否则调用 chatDataCacheManager.getStream(cacheId, 0) 返回
- EvaluationExecutionController 构造函数新增 ChatDataCacheManager、DefaultChatDataCacheProvider 两个字段注入，原 execute/getStatus 端点不变
- EvaluationExecutionService 注入 DefaultChatDataCacheProvider 字段（构造注入）
- EvaluationExecutionService.execute 方法在 asyncEvaluationExecutor.executeAsync(...) 之后添加缓存轮询：String sessionId=String.valueOf(executionSession.getId())，循环 Thread.sleep(200ms) 后调用 defaultChatDataCacheProvider.getCacheIdsBySessionId(sessionId)，列表非空则跳出返回 statusDTO；每次循环检查 executionStatusMap 中 statusDTO 是否变为 FAILED，若 FAILED 则返回 FAILED 状态 DTO；InterruptedException 时恢复中断标记并跳出循环
- EvaluationExecutionController 缓存 API 重构：删除旧 GET /session/{executionSessionId}/stream 与 GET /session/{executionSessionId}/cache/status 端点；新增 GET /cache/status?sessionId=xxx（返回 ApiResponse<Map<String,Object>> 含 hasCache 布尔值与 cacheId 字符串，cacheId 取 getCacheIdsBySessionId 第一个，无缓存时 hasCache=false、cacheId=null）、GET /cache/{cacheId}/stream（直接调用 chatDataCacheManager.getStream(cacheId, 0) 返回 SSE 流）、DELETE /cache/{cacheId}（调用 defaultChatDataCacheProvider.removeCache(cacheId) 删除缓存）
## 会话管理

- DefaultContextDataProvider 实现 ContextDataProvider.updateLastResponseId（更新 session 表 last_response_id）并在 loadAgentContext 的所有 AgentContextData 构造中透传 session.getLastResponseId()
- 已删除 DefaultSessionDataProvider（SessionDataProvider 接口已从 agent-base 移除，lastResponseId 持久化能力迁移至 ContextDataProvider）
- AgentContextDTO 新增 lastResponseId 字段；AgentContextController 在 getContext 中映射 ctx.getLastResponseId()
- 新增 GET /api/context/{sessionId}/basic 轻量接口，返回 AgentContextBasicDTO（sessionId/agentId/modelId/lastResponseId/parentSessionId）
- 新增 MessageService 消息查询服务接口与 MessageServiceImpl 实现：注入 MessageMapper，getAllMessages(Long sessionId) 查询 sessionId + rollback=false 的消息，按 sequenceNum 升序，不做 memoryPoint 记忆点过滤
- SessionServiceImpl.getMessages() 改为注入并调用 MessageService.getAllMessages() 获取消息实体列表，再通过 DefaultMessageDataProvider.toMessageDTOs 转换为 MessageDTO 返回；sessionManager.getMessages() 在 AgentContextManager 中的原有用途保持不变
- 新增 MessageService.getMessagesBySeqRange(Long sessionId, Integer startSeq, Integer endSeq) 方法：查询 sessionId + rollback=false + sequenceNum 在 [startSeq, endSeq] 区间的消息，按 sequenceNum 升序，MessageServiceImpl 注入 MessageMapper 通过 LambdaQueryWrapper（ge/le）实现
- SessionController 新增 GET /api/sessions/{id}/messages/range 端点：接收 startSeq/endSeq 查询参数，调用 MessageService.getMessagesBySeqRange 返回消息实体列表（ApiResponse<List<Message>>），注入 MessageService 依赖
- SessionController 注入 DefaultMessageDataProvider，GET /api/sessions/{id}/messages/range 端点返回类型从 ApiResponse<List<Message>> 实体列表改为 ApiResponse<List<MessageDataProvider.MessageDTO>>：调用 MessageService.getMessagesBySeqRange 获取实体后通过 defaultMessageDataProvider.toMessageDTOs 转换为 MessageDTO，与 GET /api/sessions/{id}/messages 端点返回结构对齐；SessionControllerTest 同步更新断言（mock defaultMessageDataProvider.toMessageDTOs，断言 MessageDTO 返回）
## 知识库管理

- 知识库(KnowledgeBase) CRUD 接口：DTO（KnowledgeBaseDTO/KnowledgeBaseCreateRequest/KnowledgeBaseUpdateRequest）、Service（KnowledgeBaseService 接口与 KnowledgeBaseServiceImpl 实现）、Controller（KnowledgeBaseController，路径 /api/knowledge-bases）
- 支持 name 唯一性校验（create 和 update 时检查，重复抛 KNOWLEDGE_BASE_ALREADY_EXISTS）
- 级联删除：delete 时 @Transactional 内先按 knowledgeBaseId 删除该知识库下所有 KnowledgeFile，再删除知识库
- 知识文件(KnowledgeFile) CRUD 接口：DTO（KnowledgeFileDTO/KnowledgeFileCreateRequest/KnowledgeFileUpdateRequest）、Service（KnowledgeFileService 接口与 KnowledgeFileServiceImpl 实现）、Controller（KnowledgeFileController，路径 /api/knowledge-bases/{kbId}/files）
- KnowledgeFile.create 校验 knowledgeBaseId 对应知识库存在（否则抛 KNOWLEDGE_BASE_NOT_FOUND），并按 fileContent 计算 fileSize（UTF-8 字节数）与 lineCount（按 \n 计数）
- 实体与 Mapper 复用 platform-data 模块已存在的 KnowledgeBase/KnowledgeFile/AgentKnowledgeBase 及对应 Mapper
- 知识库与知识文件均支持 toggleStatus（@RequestParam status）状态切换，list 支持 name/fileName 模糊过滤与 status 过滤
- 知识文件内容读写拆分：KnowledgeFileDTO/CreateRequest/UpdateRequest 均不含 fileContent 字段；Service 新增 getFileContent(Long id): String（返回纯文本内容）与 updateFileContent(Long id, String content): void（更新内容并按新内容重算 fileSize/lineCount），create 不再计算 fileSize/lineCount；Controller 新增 GET /{id}/content（返回 ApiResponse<String> JSON 包装的文件内容）与 PUT /{id}/content（consumes=text/plain，@RequestBody String）两个端点
- Elasticsearch 客户端配置（ElasticsearchConfig）：加载 elasticsearch.host/port/username/password 配置（默认 localhost:9200），创建 ElasticsearchTransport Bean（destroyMethod=close）与 ElasticsearchClient Bean；配置了用户名密码时通过 BasicCredentialsProvider 注入认证
- 文本块模型（TextChunk）：knowledgeBaseId/fileId/lineNumber/vector(List<Float>)/text/kbEnabled/fileEnabled 字段，knowledgeBaseId/fileId 使用 ToStringSerializer 序列化为字符串，与索引 keyword 映射一致
- 知识搜索客户端（KnowledgeSearchClient，@Service）：封装 ES 索引操作，全部方法接收 indexName 参数。createIndex 创建 mapping（vector=dense_vector similarity=cosine dim 动态、text=ik_max_word/ik_smart、kbEnabled/fileEnabled=boolean、knowledgeBaseId/fileId=keyword、lineNumber=integer）；batchSave 通过 bulk 批量保存（文档 id=knowledgeBaseId_fileId_lineNumber，重复上传幂等）；deleteByKnowledgeBase/deleteByFile 用 deleteByQuery 按 term 删除；vectorSearch 用 search.knn（KnnSearch，query_vector 为 List<Float>，按分数降序）；fullTextSearch 用 bool filter(term knowledgeBaseId)+must(match text) BM25 检索；updateEnabledByFile/updateEnabledByKnowledgeBase 用 updateByQuery + painless script 将 kbEnabled/fileEnabled 均设为 enabled；ES 调用 IOException 统一包装为 IllegalStateException
- KnowledgeSearchClient 逻辑调整：updateEnabledByFile 改为仅更新 kbEnabled、updateEnabledByKnowledgeBase 改为仅更新 fileEnabled；vectorSearch/fullTextSearch 检索条件增加 kbEnabled=true AND fileEnabled=true 过滤（knn filter 用 bool 组合、fullText 用 bool filter 组合）；batchSave/deleteByKnowledgeBase/deleteByFile/vectorSearch/fullTextSearch/updateEnabledByFile/updateEnabledByKnowledgeBase 方法开头调用私有 ensureIndex(indexName)（索引不存在时自动 createIndex）；deleteIndex 索引不存在时跳过不报错；createIndex 索引已存在时抛 IllegalStateException
- updateEnabled 字段语义修正（最终）：updateEnabledByFile 按 fileId 仅更新 fileEnabled（文件启用状态）；updateEnabledByKnowledgeBase 按 knowledgeBaseId 仅更新 kbEnabled（知识库启用状态）
- 离朱测试通过（37/37）：KnowledgeSearchClientTest 28/28（updateEnabled 字段断言已按新语义更新）、ElasticsearchConfigTest 6/6、TextChunkTest 3/3，无回归
- 知识文件发布与知识库重建索引（KnowledgePublishService，@Service + @EnableAsync）：publishFile(Long fileId) 为 @Async 异步方法，将文件 publishStatus 置为 PUBLISHING→（索引存在时）deleteByFile 清理旧文本块→读取 fileContent 按 \n 拆行并过滤空行→逐行调用 ModelInvoker.embed() 获取向量→构建 TextChunk（kbEnabled/fileEnabled 取自知识库/文件 ENABLED 状态，lineNumber 从 1 顺序编号）→batchSave 批量写入→成功置 PUBLISHED、失败置 PUBLISH_ERROR 并 deleteByFile 清理；embedding 模型从 KnowledgeBase.vectorModelId 对应 ModelConfig 的 modelName 获取，EmbeddingRequest.model 使用该 modelName；获取首个向量后若索引不存在则按其维度动态调用 createIndex(indexName, dimension)
- rebuildKnowledgeBase(Long kbId)：将知识库 rebuilding=true→deleteIndex 删除旧索引→按 knowledgeBaseId + publishStatus in (PUBLISHING/PUBLISHED/PENDING_PUBLISH) 查询需重发文件→逐个调用 publishFile 重新发布→finally 中 rebuilding=false；自调用 publishFile（类内调用）同步执行，异常时仍会复位 rebuilding
- KnowledgeBase 新增向量模型与索引字段：KnowledgeBaseDTO/CreateRequest/UpdateRequest 新增 vectorModelId（create 必填 @NotNull）、esIndex（可选）；create 时 esIndex 为空则自动生成（格式 agent_+knowledgeBaseId+_+随机6位小写字母数字下划线组合）；update 时 esIndex 为空自动生成；DTO 新增 rebuilding 字段，getById 返回 rebuilding 状态供轮询
- KnowledgeFile 新增发布状态：KnowledgeFileDTO 新增 publishStatus 字段；KnowledgeFileServiceImpl.create 新建文件 publishStatus=UNPUBLISHED；updateFileContent 若当前为 PUBLISHED 则自动置为 PENDING_PUBLISH
- KnowledgeFileController 新增 POST /{id}/publish（调用 publishFile 异步发布）与 PUT /refresh（重新查询列表，支持 fileName/status 过滤参数）；KnowledgeBaseController 新增 POST /{id}/rebuild-es（调用 rebuildKnowledgeBase）
- KnowledgeSearchClient 新增 createIndex(String indexName, int dimension) 重载：dense_vector 显式设置 dimension，维度与向量模型输出一致；原 createIndex(String) 保留不指定维度
- KnowledgeSearchClient 新增 searchByFileAndLineRange(String indexName, Long knowledgeBaseId, Long fileId, int startLine, int endLine)：使用 bool filter 过滤 knowledgeBaseId+fileId term + lineNumber range（gte/lte），返回该文件指定行范围的 TextChunk 列表，按 lineNumber 升序排列（sort lineNumber asc，size=范围跨度）；startLine>endLine 时直接返回空列表；索引不存在时自动创建
- 智能体-知识库绑定（AgentKnowledgeBase 关联）：AgentCreateRequest/AgentUpdateRequest 新增 knowledgeBaseIds (List<Long>) 字段；AgentConfigDTO 新增 knowledgeBases (List<KnowledgeBaseDTO>) 字段（每个元素含 id/knowledgeBaseId 和 name）；AgentConfigServiceImpl 注入 AgentKnowledgeBaseService 与 KnowledgeBaseMapper，create 插入 AgentConfig 后按 knowledgeBaseIds 批量插入 AgentKnowledgeBase 记录（saveBatch），update 若 knowledgeBaseIds 非空先删除旧绑定再批量插入新绑定，delete 同步清理 AgentKnowledgeBase 绑定，toDTO 通过 AgentKnowledgeBaseService 查询绑定关系并结合 KnowledgeBaseMapper.selectBatchIds 批量查询名称填充 knowledgeBases；新增 AgentKnowledgeBaseService/AgentKnowledgeBaseServiceImpl（继承 ServiceImpl 提供 saveBatch/remove 能力）
- KnowledgeSearchClient.createIndex(String indexName, int dimension) 带维度重载已实现并生效：dense_vector 显式设置 dims（与向量模型输出维度一致）；KnowledgePublishService.publishFile 获取首个向量后若索引不存在则按向量维度动态调用 createIndex(indexName, dimension)（不再仅依赖 batchSave 内部无维度 ensureIndex）
- 知识库向量模型字段：KnowledgeBaseDTO/CreateRequest/UpdateRequest 均含 vectorModelId（create 必填 @NotNull）、esIndex、rebuilding；create/update 时 esIndex 为空自动生成（agent_+knowledgeBaseId+_+随机6位）；KnowledgeBaseController 提供 POST /{id}/rebuild-es 端点；rebuilding=true 时 GET /{id} 返回 rebuilding 状态供前端轮询
- KnowledgeSearchClient 移除 createIndex(String indexName, int dimension) 重载（dense_vector 显式 dims 版本），仅保留 createIndex(String)（不指定维度，ES 自动推断）；KnowledgePublishService.publishFile 不再显式调用 createIndex(indexName, dimension)，索引不存在时由 batchSave 内部 ensureIndex 自动创建无维度索引；KnowledgeSearchClientTest 删除 3 个 dimension 测试用例，KnowledgePublishServiceTest 索引不存在场景改为验证 createIndex never() 调用（用例名同步更新为「索引不存在时不显式创建索引」）。离朱测试通过：KnowledgeSearchClientTest 33/33、KnowledgePublishServiceTest 13/13，共 46 项无回归
- 知识库查询 Provider（KnowledgeBaseQueryProviderImpl，@Component）：实现 agent-integration 的 KnowledgeBaseQueryProvider 接口，供 KnowledgeSearchTool 等 agent-integration 组件使用。getKnowledgeBaseInfo 通过 SessionMapper→AgentKnowledgeBaseMapper→KnowledgeBaseMapper 链路查询会话绑定的知识库信息；searchFiles 通过 KnowledgeFileMapper 按 kbId + fileName like 查询并按 fileContent 拆行计算 maxLineCount；searchChunks 查询 KnowledgeBase 获取 esIndex，支持 vector（ModelInvoker 获取 query 向量后 vectorSearch）/full_text（fullTextSearch）/hybrid（合并去重）三种搜索类型，支持 fileId 过滤，按文件分组后调用 searchByFileAndLineRange 按 contextLines 扩展并去重上下文行块；getFileChunks 通过 searchByFileAndLineRange 查询指定行范围文本块。注入 SessionMapper/AgentKnowledgeBaseMapper/KnowledgeBaseMapper/KnowledgeFileMapper/ModelConfigMapper/ModelInvokerManager/KnowledgeSearchClient 依赖
- KnowledgeBaseQueryProviderImpl 适配 SearchType 枚举：searchChunks 的 searchType 参数从 String 改为 agent-integration 的 SearchType 枚举（VECTOR/FULLTEXT/HYBRID），search() 内部 switch 枚举分支分发（VECTOR→ModelInvoker 向量化后 vectorSearch、FULLTEXT→fullTextSearch、HYBRID→向量+全文合并去重）；searchChunks/getFileChunks 构造的 TextChunkWithFile 传入 knowledgeBaseId（record 构造顺序 knowledgeBaseId/fileId/fileName/chunkList）；getFileChunks 返回单个 TextChunkWithFile；searchChunks 新增 mergeLineRanges 行范围合并优化（重叠/相邻合并后批量查询，避免逐块 N+1）；searchFiles 使用 wrapper.last("LIMIT n") 限制返回条数。离朱测试通过：KnowledgeBaseQueryProviderImplTest 27/27、知识库相关回归 116/116
- KnowledgeBaseQueryProviderImpl.searchChunks 重构：移除 contextLines 参数与上下文扩展逻辑（mergeLineRanges 行范围合并及 searchByFileAndLineRange 上下文查询已删除），方法签名对齐 agent-integration KnowledgeBaseQueryProvider 接口的 5 参（kbId/fileId/searchType/query/topK），仅保留纯搜索+按文件分组返回命中文本块（组内按 lineNumber 去重），上下文扩展职责交由 agent-integration 的 KnowledgeSearchTool 通过 getFileChunks 完成；KnowledgeBaseQueryProviderImplTest 移除 mergedRanges/nonAdjacentRanges 上下文扩展用例，上下文扩展去重用例改为组内按行号去重
- KnowledgeBaseQueryProviderImpl 接口方法 ID 类型改为 String（对齐 agent-integration 的 KnowledgeBaseQueryProvider 接口）：searchFiles/searchChunks/getFileChunks 的参数从 Long 改为 String，getKnowledgeBaseInfo 保持 String；内部用 IdConverter.parse() 将 String 转 Long 后调用数据库/ES 层查询（knowledgeBaseMapper.selectById、knowledgeFileMapper.selectList、knowledgeSearchClient.vectorSearch/fullTextSearch/searchByFileAndLineRange）；查询结果中的 Long id 用 IdConverter.toString() 转为 String 构造 record（KnowledgeBaseInfo.kbId/FileInfo.fileId/TextChunkWithFile.knowledgeBaseId 与 fileId）返回。同步更新 KnowledgeBaseQueryProviderImplTest（27 用例 String ID 入参与断言）与 DefaultToolDataProviderTest（新增 RAG_KNOWLEDGE 搜索工具以 String ID 调用 Provider 用例）。离朱测试通过：编译 0 错误，KnowledgeBaseQueryProviderImplTest 27/27、DefaultToolDataProviderTest 28/28，全量回归 716 通过（2 失败为既有 ToolDetailDTOTest/ToolConfigServiceImplTest id 断言 Long 问题，与本变更无关）
## 消息分发

- 消息分发链路已从 platform-app 移除：MessageHandler/MessageDispatcher/ConversationIdMessageHandler 及对应测试已删除；AgentContextConfiguration.agentAssembler() 不再接收 MessageSender 参数（第 7 参传 null），platform-app 不再装配消息分发链路。MessageSender 接口仍保留在 agent-base（sendmessage 包），由 agent-base 内部按需使用
## 智能体日志

- 新增 DatabaseAgentLog（实现 agent-base 的 AgentLog 接口，@Service）：addLog 将 LogData 序列化为 JSON 存入 agent_log.log_data，从 ContextLogData 的 context 提取 sessionId（IdConverter 转 Long，解析失败返回 null）与 conversationId 写入对应列，logType/logLevel 存储枚举 code 值，通过 AgentLogMapper 持久化
- 新增 AgentLogDTO（含 id/sessionId/sessionName/conversationId/logType/logLevel/logData/createTime 字段，sessionName 为会话名）
- 新增 AgentLogService 接口与 AgentLogServiceImpl 实现：list 方法支持 sessionId/conversationId/logType/logLevel 筛选 + orderByDesc(createTime) 分页查询（Page 分页，默认 page=1、size=20），返回时通过 SessionMapper.selectBatchIds 批量查询 Session 关联获取 sessionName；cleanupExpiredLogs 为 @Scheduled(cron="0 0 1 * * ?") 定时任务，删除 createTime 早于当前时间 30 天的日志记录
- 新增 AgentLogController（GET /api/agent-logs），接收 sessionId/conversationId/logType/logLevel/page/size 查询参数，返回 ApiResponse<PageResult<AgentLogDTO>>
- AgentContextConfiguration.chatService() @Bean 增加 DatabaseAgentLog 参数，在 agentAssembler.build() 之后调用 agentAssembler.setAgentLog(databaseAgentLog) 将日志实现注册到 AgentComponentRegistry，供 agent-base 各 Service 记录日志
- AgentLogService.list 与 AgentLogController 新增 sessionName 参数（按会话名模糊搜索）：sessionName 非空时先通过 SessionMapper.selectList(like Session.title) 查询匹配的 sessionId 集合，集合为空则直接返回空分页结果，否则在日志查询 wrapper 中追加 in(sessionId, sessionIds) 过滤；sessionName 为空白时不查询 Session，不影响原筛选逻辑
- AgentLogDTO 新增 sessionVariables/conversationVariables 字段（会话变量/对话变量的 JSON 字符串），AgentLogServiceImpl.toDTO 同步映射
- AgentLogServiceImpl.loadSessionNames 会话名兜底：Session.title 为 null 或空字符串时使用 String.valueOf(Session.getId()) 作为会话名，避免 sessionName 为空
- DatabaseAgentLog.addLog 增强：当 logData 非 ContextLogData 时，通过反射调用 getSessionId()/getConversationId() 方法提取 sessionId（IdConverter 转 Long）与 conversationId（方法不存在或调用失败返回 null，不抛异常）；当 logData 为 ContextLogData 时，从 AgentExecutionContext 通过 getSessionVariableKeys+getSessionVariable 与 getConversationVariableKeys+getConversationVariable 分别构建会话变量/对话变量 Map，JSON 序列化后存入 entity 的 sessionVariables/conversationVariables 列
- DatabaseAgentLog.addLog 适配新 LogData 类层次：新增 SessionLogData 子类处理分支（instanceof SessionLogData 直接提取 sessionId/conversationId，SessionErrorLogData 等无执行上下文日志适用），保留 ContextLogData 分支（从 context 提取，含会话/对话变量序列化）及反射兜底分支（ContextBuildLogData 等直接继承 LogData 的类型）
- serializeLogData 通过 Jackson mix-in 排除冗余字段：新增 LogDataMixin（@JsonIgnoreProperties 排除 logLevel，日志级别已单独存储于 log_level 列）与 ContextLogDataMixin（排除 context，执行上下文冗余且体积大，会话/对话 ID 已单独提取），DatabaseAgentLog 构造函数复制注入的 ObjectMapper 并注册两个 mix-in（不污染共享 Spring Bean）
- 测试期缺陷修复：ContextLogDataMixin 的 @JsonIgnoreProperties 需同时包含 logLevel 与 context（Jackson 在类层次上取最近派生注解且不跨层合并，若仅排除 context 会覆盖父类 LogData 的 logLevel 排除规则，导致 ContextLogData 子类 logData JSON 中 logLevel 未排除）；DatabaseAgentLogTest 10 用例全部通过
- serializeLogData 进一步排除会话字段：新增 SessionLogDataMixin（@JsonIgnoreProperties({"logLevel","sessionId","conversationId"})），DatabaseAgentLog 构造函数注册 objectMapper.copy().addMixIn(SessionLogData.class, SessionLogDataMixin.class)，使 SessionLogData 子类（SessionErrorLogData 等）的 logData JSON 不再冗余包含会话 ID/对话 ID（已单独存储于 agent_log 表对应列）
- addLog 反射兜底移除：非 ContextLogData/非 SessionLogData 时不再通过反射调用 getSessionId()/getConversationId()（已删除 invokeGetter 私有方法及 java.lang.reflect.Method import），改为按 logType() 强转——当 logData.logType()==LogType.CONTEXT_BUILD 时强转为 ContextBuildLogData 提取 getSessionId()（ContextBuildLogData 无 conversationId，不设置该列）
## 聊天数据缓存

- AgentContextConfiguration 新增独立 @Bean defaultChatDataCacheProvider() 返回 new DefaultChatDataCacheProvider()
- chatDataCacheManager Bean 注入 DefaultChatDataCacheProvider 参数使用（不再内联 new），DatabaseAgentLog 非 null 时调用 cacheManager.setAgentLog 注入 agentLog，返回 cacheManager
- DefaultChatDataCacheProvider.CacheEntry 新增 createdAtMillis（long）字段，构造时记录 System.currentTimeMillis()
- DefaultChatDataCacheProvider 新增 getCacheIdsBySessionId(String sessionId) 方法：遍历 caches 过滤出该 sessionId 的条目，按 createdAtMillis 升序排序（同毫秒按 conversationId 字典序兜底），返回 cacheId 列表
## 智能体记忆功能

- 新增 SessionMemoryService（com.ghost616.platform.service.memory，@Service）：@Scheduled(cron="0 0 1 * * ?") 每天凌晨 1 点执行 aggregateSessionMemories——(a) 查询所有 memoryEnabled=true 的智能体并加载其全部非评估会话；(b) 通过 messageMapper 查询会话最后一条消息 sequenceNum 作为新记忆点；(c) 记忆点不变（newPoint<=oldPoint）则跳过；取新旧记忆点间的消息（rollback=false、seq>oldPoint 且 <newPoint）；(d) buildMemoryDocuments 流程：消息按 role=user 分组成轮次后，每组用 extractGroupContent 直接提取原始内容（拼接每条消息「【role】: content」后直接 trim 返回，不做截断，不调用 LLM）；再调用 LLM 对所有组内容做主题归类（classifyTopics，SYSTEM_TOPIC_CLASSIFY_PROMPT 含约束「仅相邻片段可归为同一主题，不相邻的相同主题片段也必须使用不同标签以保持连续」；输出「序号. 主题」行格式，parseTopics 用静态常量 TOPIC_LINE_PATTERN 正则解析，LinkedHashMap<序号,主题> 收集并按序号 1..N 顺序映射——LLM 乱序输出时也能正确对应，过滤空白主题标签，序号不完整或主题数量与组数不匹配时回退为每组单独成组）；按主题将同类组合并为大组（mergeByTopic 连续合并：遍历 groupSummaries 时 currTopic 变化即切分新组，不依赖 LinkedHashMap 全局合并，不相邻的同主题片段不再合并）；每个大组调用 LLM 做汇总摘要（summarizeTopicGroup，归类失败降级时跳过该汇总调用、直接使用原组原始内容作为文档文本）；每类生成一个 SessionMemoryDocument，aggregationStartSeq/aggregationEndSeq 为该大组覆盖的完整消息区间（组内第一组 startSeq 到最后一组 endSeq）；(e) 汇总摘要调用向量模型（agentConfig.vectorModelId 对应 ModelConfig 的 modelName）embed() 向量化；(f) 文档经 SessionMemoryESClient.batchSave 写入 ES 索引 session_memory，成功后将 session.memoryPointSequenceNum 更新为新记忆点；单个会话处理失败重试 5 次（含第 5 次），仍失败则 SLF4J 记录 error 日志；向量模型未配置、LLM/向量模型不可用、组原始内容/大组汇总或向量化为空时跳过该组/该会话

- SessionMemoryService.resolveNewMemoryPoint 重构（记忆点计算逻辑）：通过 messageMapper.countUserMessages(sessionId) 获取会话 user 消息组总数 totalGroups，结合 agentConfig.memoryGroupCount 计算 skipGroups=totalGroups-memoryGroupCount；skipGroups<=0 时返回 null（无新消息可归档，跳过聚合）；否则调用 messageMapper.findNthUserSequenceNum(sessionId, skipGroups) 获取新记忆点（第 skipGroups+1 个 user 消息的 sequenceNum），归档该序号之前的消息；queryMessagesBetween 边界条件由 <= newPoint 改为 < newPoint（lt），避免归档边界消息与记忆点消息重叠（记忆点消息本身保留在会话上下文）
- processSession 日志补充 memoryPoint 字段，endSeq 取最后一个记忆文档的 aggregationEndSeq（实际归档的最后消息序号）
- SessionMemoryService.buildMemoryDocuments 重构为「组内容提取→主题归类→同类合并→大组汇总→文档生成」流程：新增 GroupSummary record（startSeq/endSeq/summary，summary 现为组原始内容）、SYSTEM_TOPIC_CLASSIFY_PROMPT（主题归类提示词）、SYSTEM_GROUP_SUMMARY_PROMPT（大组汇总提示词）、invokeLlm 统一封装 LLM 调用、classifyTopics/parseTopics/mergeByTopic/summarizeTopicGroup 私有方法；每类生成一个 SessionMemoryDocument，aggregationStartSeq/aggregationEndSeq 为该类覆盖的完整消息区间（大组内第一组 startSeq 到最后一组 endSeq）
- SessionMemoryService.parseTopics 修复：① 序号升序校验/乱序映射——parseTopics 用 LinkedHashMap<序号,主题>（topicByIndex）收集，序号 1..expectedCount 完整且 size 匹配时按 1..N 顺序输出主题列表，LLM 乱序输出（如「2. 主题\n1. 主题」）按序号映射而非按顺位匹配；② Pattern.compile 提取为静态常量 TOPIC_LINE_PATTERN；③ 过滤空白主题标签（匹配行主题部分 trim 后为空则跳过该行，收集后 size 不足 → 返回空 → 回退每组单独成组）；④ 归类失败降级时跳过额外的大组汇总 LLM 调用——classificationFailed（topics 为 null 或数量不匹配）时对 size==1 的 topicGroup 直接使用 topicGroup.get(0).summary() 作为文档文本，不再调用 summarizeTopicGroup
- SessionMemoryService.buildMemoryDocuments 优化（移除 summarizeGroup 逐组 LLM 摘要）：新增 extractGroupContent 私有方法直接提取消息组原始内容（拼接「【role】: content」并 trim，不调用 LLM），替代原 summarizeGroup 逐组 LLM 摘要步骤，减少 N 次 LLM 调用；删除 SYSTEM_SUMMARY_PROMPT 常量与 summarizeGroup 方法；GroupSummary.summary 字段语义从「LLM 组概要」变为「组原始内容」；同步更新 SessionMemoryServiceTest 的 LLM 调用次数断言（单组 3→2、同主题 4→2、不同主题 5→3、归类失败降级 3-5→1、乱序 4→2），归类失败降级时文档 aggregationText 直接为组原始内容（如「【user】: q1\n【assistant】: a1」）
- SessionMemoryService 主题连续合并（相邻约束 + mergeByTopic 连续切分）：① SYSTEM_TOPIC_CLASSIFY_PROMPT 添加约束「仅相邻片段可归为同一主题，不相邻的相同主题片段也必须使用不同标签以保持连续」；② mergeByTopic 改为连续合并逻辑——遍历 groupSummaries 时用 currentTopic 记录当前主题，主题变化即切分新组（不再使用 LinkedHashMap 按主题全局合并），不相邻的同主题片段（A→B→A）不再合并，各自切分为独立大组；新增测试用例 aggregate_sameTopicNonAdjacent_splitByContiguity（A→B→A 切分 3 段，3 个文档，LLM 调用 classify+3×summarize=4 次）；离朱测试通过 13/13，相关模块回归无异常
- SessionMemoryService.extractGroupContent 移除字符截断：删除 MAX_GROUP_CONTENT_CHARS=500 常量，extractGroupContent 拼完消息内容后直接 content.toString().trim() 返回，不再按最大字符数截断加省略号；离朱测试通过 13/13，相关模块回归无异常
- SessionMemoryService 新增 triggerSessionMemory(Long sessionId) 手动触发方法：校验会话存在（否则抛 SESSION_NOT_FOUND）、智能体存在（否则抛 AGENT_NOT_FOUND）、memoryEnabled=true（否则抛 AGENT_MEMORY_NOT_ENABLED），校验通过后通过 CompletableFuture.runAsync 异步执行 processSessionWithRetry 生成记忆文档；SessionController 新增 GET /api/sessions/{id}/memory/trigger 端点调用该方法，返回「记忆摘要生成已触发」
- SessionMemoryDocument 新增 aggregationStartTime(Long)/aggregationEndTime(Long) 字段（聚合起始/结束毫秒时间戳）；SessionMemoryService 的 GroupSummary record 新增 startTime/endTime（Long，毫秒时间戳）字段，buildMemoryDocuments 中从每组首/尾消息的 createTime 通过 toEpochMillis（LocalDateTime.atZone(systemDefault).toInstant().toEpochMilli()，null 返回 null）转换赋值，构建 SessionMemoryDocument 时填充 aggregationStartTime=topicGroup 首组 startTime、aggregationEndTime=末组 endTime；SessionMemoryESClient.createIndex mapping 新增 aggregationStartTime/aggregationEndTime 两个 long 类型字段

- 记忆聚合按天模式（aggregationType 字段）：SessionMemoryDocument 新增 aggregationType（com.ghost616.platform.enums.AggregationType 枚举，GROUP/DAILY，ES mapping 存 keyword）；SessionMemoryESClient.createIndex mapping 新增 aggregationType/keyword 字段，buildDocId 调整为 sessionId_aggregationType_startSeq_endSeq（避免 GROUP/DAILY 文档 ID 冲突）；SessionMemoryESClient 新增 checkDailyExists(String sessionId, long startTime, long endTime) 方法：bool 查询 term sessionId + term aggregationType=DAILY + range aggregationStartTime lte endTime + range aggregationEndTime gte startTime（重叠判断），size=1，命中返回 true
- SessionMemoryService 聚合双通道：processSession 拆分为 processGroupAggregation（原分组聚合逻辑，aggregationType=GROUP）与 processDailyAggregation（按日聚合）；processDailyAggregation 以当前时间 Instant.now() 截断到秒为基准 endTime、往前推 24h 得 startTime，先 checkDailyExists 防重（已存在跳过），再通过 queryMessagesByTimeRange（eq sessionId + eq rollback=false + createTime ge startTime 且 le endTime，按 sequenceNum 升序）查询时间段内消息，走相同 buildMemoryDocuments 流程（buildMemoryDocuments 新增 aggregationType 与 windowStartTime/windowEndTime 参数，DAILY 时聚合文档的 aggregationStartTime/aggregationEndTime 直接填充为窗口时间戳范围），生成 DAILY 文档 batchSave 写入；triggerSessionMemory 与 aggregateSessionMemories 均通过 processSessionWithRetry 同时触发分组聚合与按日聚合