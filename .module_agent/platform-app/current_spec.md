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