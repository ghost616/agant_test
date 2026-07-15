# agent-integration 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## agent-integration 模块功能说明

- **AgentAssembler**：Agent 组件组装类，build() 方法不再调用 chatService.initHooks()，hooks 初始化移至 AgentContextConfiguration 的 chatService Bean 中显式执行，以解决 MessageSavePostHook 创建时的 Bean 时序依赖问题
- **AgentAssembler**：build() 方法构造 ChatDataProviderProxy 代理，将 MessageSavePostHook 通过代理注入到 ChatService；暴露 messageSavePostHook() getter 并在 Result record 中包含该实例
- **AgentAssembler**：build() 方法构造 SystemToolProviderProxy 代理，确保 history_query/load_skills/unload_skills 三个系统工具始终可用；SystemToolManager 使用代理而非原始 SystemToolProvider
- **AgentAssembler**：build() 方法内部使用 AgentComponentRegistry 统一装配所有 Provider/Manager/Tracker，registry 不对外暴露；对外 getter 方法签名不变
## 模块职责
提供多平台模型调用器的实现（ModelInvoker）和 Agent 组件的组装能力。

## 核心功能

### 模型调用器（ModelInvoker 实现）
- **OpenAIInvoker**：OpenAI 兼容平台的模型调用，支持同步 invoke、流式 invokeStream、模型 verify 和工具定义转换
- **OllamaInvoker**：Ollama 本地模型的调用，支持同步/流式模式
- **AnthropicInvoker**：Anthropic Claude 模型的调用，使用 SSE 事件流解析
- **AzureInvoker**：Azure OpenAI 服务调用，继承 OpenAIInvoker 并覆盖 API URL
- **DeepSeekInvoker**：DeepSeek 平台调用（OpenAI 兼容协议）
- **CustomInvoker**：自定义通用 OpenAI 兼容端点调用

### 工厂与组装
- **DefaultModelInvokerFactory**：根据平台类型（OPENAI/ANTHROPIC/AZURE/OLLAMA/DEEPSEEK/CUSTOM）创建对应 Invoker
- **Build**：接收 DataProvider 和 ModelInvokerFactory 依赖，组装完整的 ChatService 和 ToolExecutionService 实例

- **SubSessionCallbackSystemTool**：实现 SystemTool 接口的系统工具。工具名 callback_sub_session，通过构造函数注入 SubSessionCallback 回调，支持按名称列表匹配工具和技能创建子会话，并通过回调执行用户消息返回结果。
