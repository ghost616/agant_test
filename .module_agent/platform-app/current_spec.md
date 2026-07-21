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