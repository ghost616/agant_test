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
- **AgentAssembler**：build() 方法中创建共享 HookManager 实例，通过 setter 注入 ChatService 和 ToolExecutionService，并统一调用 hookManager.refreshHooks(chatDataProvider.getHooks())，将 hooks 初始化从 AgentContextConfiguration 移至 build() 中统一管理
- **AgentAssembler**：新增私有字段 hookManager 及公开方法 refreshHooks()，将刷新操作从 build() 中解耦，支持事后调用刷新
- **ChatDataProviderProxy**：实现 ChatDataProvider.getHooks(Long sessionId) 方法，直接委托给 delegate.getHooks(sessionId)
- **AgentAssembler**：refreshHooks() 方法中更改 hookManager.refreshHooks(chatDataProviderProxy.getHooks()) 为 hookManager.refreshHooks() 无参调用；HookManager 构造函数改为 new HookManager(registry)
- **AgentAssembler**：移除 messageSavePostHook() 公开 getter 方法（无人调用）；Result record 和 ChatDataProviderProxy 内部仍保留对 MessageSavePostHook 的引用
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

### 浏览器工具（Browser Tool）
- **BrowserToolCallback**：函数式接口，定义 `execute(String sessionId, String toolId, String toolName, String toolParams)` 方法，用于浏览器工具的回调执行
- **BrowserToolInvoker**：继承 CustomToolInvoker 的自定义工具调用器。构造函数注入 BrowserToolCallback 回调，execute() 从 AgentExecutionContext 获取 sessionId、从 ToolConfigDTO 获取 toolId/toolName，将参数 JSON 传递给回调执行；提供 loadJsContent() 方法从 classpath 加载 browser_tool_executor.js、getJsContent() 返回缓存的 JS 内容
- **browser_tool_executor.js**：JS 工具执行引擎，定义 AgentExecutionContext 对象、ToolFunction 工具函数定义、ToolManager 工具函数管理器（按 toolName 绑定/添加/移除/get）、四个工具执行函数：getAgentExecutionContext 获取上下文、getToolResult 从管理器获取工具执行结果、passToolResult 回传结果给宿主、execute 主执行入口

- **OpenAIResponsesInvoker**：OpenAI Responses API 模型调用器，实现 ModelInvoker 接口，使用 /v1/responses 端点。同步 invoke 使用 instructions+input 请求格式，解析 output 数组提取 message content（output_text）与 function_call 到 ChatResponse（含 responseId 供多轮续接）；流式 invokeStream 解析 SSE 事件：response.output_text.delta→delta、response.function_call_arguments.delta→toolCalls（index+arguments）、response.web_search_call.in_progress/searching/completed→webSearchCall（completed 时从 results 数组解析 title/url/snippet）、response.completed→finishReason+usage；verify() 使用 GET /v1/models；toToolDefinition() 与 OpenAIInvoker 一致。input 消息转换：system 跳过、user/assistant 普通文本、assistant 工具调用转为 content 中的 function_call 部件、tool 角色转为 function_call_output
- **OpenAIResponsesInvoker.buildBuiltinTools(ChatRequest)**：受保护方法从 request.builtinTools 读取内置工具列表（未传或为空返回空列表，请求不传则不启用）；buildRequestBody() 在 buildTools(request.getTools()) 之后将内置工具合并到 tools 数组，仅当 tools 非空时写入 body；子类（DeepSeek/Kimi/VolcEngine/Azure/Custom）不覆写该方法，统一由父类处理

- **DefaultModelInvokerFactory**：createInvoker() 先判断 requestType（responses/responses_stateless）再按 platformType 路由到对应 ResponsesInvoker（OPENAI/DEEPSEEK/KIMI/VOLCENGINE/AZURE/CUSTOM），否则走原 switch 返回 Chat Completions Invoker；ANTHROPIC/OLLAMA 不参与 Responses 路由，保持原分支不变
- **DeepSeekResponsesInvoker**：DeepSeek 平台 Responses API 调用器，继承 OpenAIResponsesInvoker，无额外覆写
- **KimiResponsesInvoker**：Kimi 平台 Responses API 调用器，继承 OpenAIResponsesInvoker；覆写 buildRequestBody 保留模型适配逻辑：K2_7_CODE 模型移除 reasoning，K3 模型将 thinking 映射为 reasoning（effort=max）
- **VolcEngineResponsesInvoker**：火山引擎平台 Responses API 调用器，继承 OpenAIResponsesInvoker，无额外覆写
- **AzureResponsesInvoker**：Azure OpenAI 平台 Responses API 调用器，继承 OpenAIResponsesInvoker；覆写 buildResponsesUrl=mountResponsesResourceUrl 使用 Azure 部署资源路径 + api-version，invoke/invokeStream/verify 使用 api-key header 认证
- **CustomResponsesInvoker**：自定义平台 Responses API 调用器，继承 OpenAIResponsesInvoker，无额外覆写
- **OpenAIResponsesInvoker.parseStreamEvent 新增事件**：response.reasoning_text.delta 解析 delta 写入 ChatChunk.reasoning；response.custom_tool_call.in_progress/done 解析 item_id/output_index/input 构建 ChatChunk.CustomToolCall 写入 customToolCall
## 工厂与组装

- **DefaultModelInvokerFactory**：根据平台类型（OPENAI/ANTHROPIC/AZURE/OLLAMA/KIMI/VOLCENGINE/DEEPSEEK/CUSTOM/SILICONFLOW）创建对应 Invoker；createChatCompletionsInvoker 新增 SILICONFLOW case 分支创建 SiliconFlowInvoker；supportsResponses 中不包含 SILICONFLOW（SILICONFLOW 仅支持 chat-completions，不支持 Responses API，与前端 RESPONSES_SUPPORTED 保持一致）
## 模型调用器（ModelInvoker 实现）

- **OpenAIInvoker**：新增 embed(EmbeddingRequest) 方法，调用 baseUrl+/embeddings 接口，请求体构建 model + input/inputList（inputList 优先；input 与 inputList 均为 null 时抛出 IllegalArgumentException），解析响应返回 EmbeddingResponse（embeddings 列表与 usage），错误处理遵循 invoke() 模式；新增 buildEmbeddingsUrl/buildEmbeddingRequestBody/parseEmbeddingResponse 受保护方法
## 知识库查询工具

### 知识库查询工具（Knowledge Base Query Tools）
- **SearchType**：知识库文本块搜索类型枚举（VECTOR/FULLTEXT/HYBRID），用于 searchChunks 的搜索类型参数。
- **KnowledgeBaseQueryProvider**：知识库查询 Provider 接口，定义四类查询能力：getKnowledgeBaseInfo(sessionId) 获取会话关联知识库信息列表（返回 List\<KnowledgeBaseInfo\>，会话或知识库不存在时返回空列表）、searchFiles(kbId, fileName, limit) 按文件名搜索文件（返回 List\<FileInfo\>，仅返回已发布到 ES 的文件）、searchChunks(kbId, fileId, searchType, query, topK) 搜索文本块（返回 List\<TextChunkWithFile\>，searchType 为 SearchType 枚举，不含上下文扩展，返回纯匹配结果；参数 fileId 作为 ES 查询过滤条件在查询层面生效（非内存过滤），非 null 时仅返回该文件下的文本块）、getFileChunks(kbId, fileId, startLine, endLine) 获取行号范围文本块（返回 TextChunkWithFile）。由外部模块提供实现。
- **KnowledgeBaseInfo/FileInfo/TextChunkWithFile**：知识库查询数据类。KnowledgeBaseInfo 含 kbId/kbName/kbDescription；FileInfo 含 fileId/fileName/fileDescription/maxLineCount；TextChunkWithFile 含 knowledgeBaseId/fileId/fileName/chunkList，嵌套 TextChunk(lineNumber/text)。
- 四个知识库工具类均继承 CustomToolInvoker（非 SystemTool），无 @Component 注解，构造函数传参（ToolConfigDTO + KnowledgeBaseQueryProvider），提供静态 createToolConfig() 返回 ToolConfigDTO（id=null, toolType=CUSTOM），工具名/描述/参数 schema 定义在 ToolConfigDTO 中：
  - **KnowledgeBaseInfoTool**（default_tool_rag_info）：无参数，execute 通过 ctx.getSessionId() 获取会话 ID 调用 getKnowledgeBaseInfo 返回知识库信息列表 JSON（null 时序列化为 []）。
  - **KnowledgeFileInfoTool**（default_tool_rag_file_info）：参数 knowledgeBaseId(必填)/fileId(可选)/fileName/searchLimit(默认10)，调用 searchFiles 返回文件列表 JSON，传 fileId 时按 fileId 过滤。
  - **KnowledgeSearchTool**（default_tool_rag_search）：参数 knowledgeBaseId(必填)/fileId/searchType(必填，enum VECTOR/FULLTEXT/HYBRID)/query(必填)/searchLimit(默认10)/contextLines(默认3)。调用 searchChunks 获取纯匹配结果后，用 contextLines 扩大每个 chunk 的行范围（line-contextLines ~ line+contextLines，下限 1），将同文件重叠/相邻行范围经 mergeRanges 合并后逐个调用 provider.getFileChunks() 获取上下文文本块；随后按 (knowledgeBaseId, fileId) 分组到 LinkedHashMap，组内按 lineNumber 去重（LinkedHashMap putIfAbsent 保持插入顺序）后按行号升序合并连续行号块，返回 List\<{knowledgeBaseId, fileId, chunks}\> 结构。
  - **KnowledgeFileChunkTool**（default_tool_rag_file_chunk）：参数 knowledgeBaseId(必填)/fileId(必填)/startLine(默认0)/endLine(默认文件最大行数)。endLine 未传时通过 searchFiles 解析文件 maxLineCount，找不到则用 Integer.MAX_VALUE，调用 getFileChunks 后将 chunkList 按行号升序合并为纯文本字符串返回（块之间以换行分隔，null/空列表返回空字符串）。
- 以上工具错误 JSON 序列化使用 JsonMapper。