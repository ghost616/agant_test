# agent-integration 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## agent-integration 模块功能说明

- **AgentAssembler**：Agent 组件组装类，build() 方法不再调用 chatService.initHooks()，hooks 初始化移至 AgentContextConfiguration 的 chatService Bean 中显式执行，以解决 MessageSavePostHook 创建时的 Bean 时序依赖问题
- **AgentAssembler**：build() 方法构造 ChatDataProviderProxy 代理，将 MessageSavePostHook 通过代理注入到 ChatService；暴露 messageSavePostHook() getter 并在 Result record 中包含该实例
- **AgentAssembler**：build() 方法构造 SystemToolProviderProxy 代理，确保 history_query/load_skills/unload_skills 三个系统工具始终可用；SystemToolManager 使用代理而非原始 SystemToolProvider
- **AgentAssembler**：build() 方法内部使用 AgentComponentRegistry 统一装配所有 Provider/Manager/Tracker，registry 不对外暴露；对外 getter 方法签名不变
- **SubSessionCallbackSystemTool**：参数 schema 新增 thinking（boolean，可选）；execute() 解析工具参数 JSON 中的 thinking 字段（默认 null）并传递给 createChildSession 方法
- **SubSessionCallbackSystemTool**：execute() 方法改为通过构造函数注入的 SubSessionCallback 回调发送子会话消息，而非直接调用 ctx.sendUserMessage()；thinking 参数通过回调的第三个参数传递
- **AgentAssembler**：构造函数新增 MessageSender 参数（可为 null），build() 中调用 registry.setMessageSender(messageSender) 传入 MessageSender 实例
- **AgentAssembler**：build() 方法构造 SystemToolProviderProxy 代理，确保 history_query/load_skills/unload_skills/session_variable/conversation_variable 五个系统工具始终可用；SystemToolManager 使用代理而非原始 SystemToolProvider
## 模块职责
提供多平台模型调用器的实现（ModelInvoker）和 Agent 组件的组装能力。

## 核心功能

### 模型调用器（ModelInvoker 实现）
- **OpenAIInvoker**：OpenAI 兼容平台的模型调用，支持同步 invoke、流式 invokeStream、模型 verify 和工具定义转换
- **OllamaInvoker**：Ollama 本地模型的调用，支持同步/流式模式
- **AnthropicInvoker**：Anthropic Claude 模型的调用，使用 SSE 事件流解析
- **AzureInvoker**：Azure OpenAI 服务调用，继承 OpenAIInvoker 并覆盖 API URL
- **DeepSeekInvoker**：DeepSeek 平台调用（OpenAI 兼容协议）
- **KimiInvoker**：Kimi 平台调用（OpenAI 兼容协议，继承 OpenAIInvoker）
- **VolcEngineInvoker**：火山引擎平台调用（OpenAI 兼容协议，继承 OpenAIInvoker）
- **CustomInvoker**：自定义通用 OpenAI 兼容端点调用

### 工厂与组装
- **DefaultModelInvokerFactory**：根据平台类型（OPENAI/ANTHROPIC/AZURE/OLLAMA/KIMI/VOLCENGINE/DEEPSEEK/CUSTOM）创建对应 Invoker
- **Build**：接收 DataProvider 和 ModelInvokerFactory 依赖，组装完整的 ChatService 和 ToolExecutionService 实例

- **SubSessionCallbackSystemTool**：实现 SystemTool 接口的系统工具。工具名 callback_sub_session，通过构造函数注入 SubSessionCallback 回调，支持按名称列表匹配工具和技能创建子会话，并通过回调执行用户消息返回结果。
### Usage 导出到流式 Chunk
- **OpenAIInvoker.parseStreamChunk**：从流式 JSON 根节点解析 usage（prompt_tokens/completion_tokens/total_tokens），设置到 ChatChunk.usage 字段
- **AnthropicInvoker.invokeStream**：通过 usageHolder 捕获 message_delta 事件中的 usage（input_tokens/output_tokens），在最终 stop chunk 中设置 ChatChunk.usage
- **OllamaInvoker.parseStreamChunk**：在 done=true 的最终 chunk 中解析 eval_count/prompt_eval_count，设置到 ChatChunk.usage