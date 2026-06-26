智能体执行引擎：对话编排、工具调度、HOOK 触发、上下文管理、流式 SSE 推送
## 数据模型

- SessionService: 会话服务接口，定义 listSessions、createSession、getSession、deleteSession 和 getMessages 方法
- SessionServiceImpl: 会话服务实现，getSession 通过 selectById 查询并校验非空后 toDTO 返回
- SessionController: 会话 REST 控制器（/api/sessions），提供 GET 列表查询、GET /{id} 单会话查询、POST 创建、GET /{id}/messages 消息列表和 DELETE 逻辑删除接口
ChatService.java: 构建 system 消息时对 context.getSystemPrompt() 增加空值判断，若为 null 设为空字符串 ""，避免发送 `"content": null` 给 LLM API。
JavaToolInvoker.java: 实现 ToolInvoker 接口，构造时接收全限定类名和 ApplicationContext，优先通过 Spring Bean 获取实现类实例，fallback Class.forName().newInstance() 反射实例化；execute() 委托给加载的实现类执行。
ToolManager.java: Spring @Component，注入 ApplicationContext 和 ToolConfigService，提供 getInvoker(String toolName) 按工具名称查询 ToolConfigDTO，对 JAVA 类型使用 JavaToolInvoker 加载；提供 execute(ToolInvoker, ...) 方法封装调用。
SessionServiceImpl.createSession(): 在 sessionMapper.insert(entity) 之后查询 agent_tool 表获取该智能体关联的所有工具 ID，为每个工具创建 SessionTool 记录（含 sessionId 和 toolId）并逐个插入 session_tool 表，确保创建会话后 AgentContextManager.getOrCreate() 能通过 sessionManager.getSessionTools() 查询到会话工具列表。
SessionServiceImpl.createSession() 添加 @Transactional 保证原子性；使用 SessionToolMapper.insertBatch(List) 批量插入替代逐条 insert；deleteSession() 新增删除 session_tool 记录避免孤儿数据；变量名 at/st 改为 agentTool/sessionTool 提高可读性。
## 实体定义

Message 实体对应 message 表，不继承 BaseEntity（无 update_time/deleted 列）。字段：id (雪花ID)、sessionId、role、content、reasoning、sequenceNum、toolCallId、createTime。
## AgentContextManager

AgentContextManager.java: Spring @Component 上下文缓存管理器，使用 ConcurrentHashMap&lt;Long, AgentSessionContext&gt; 缓存上下文实例。注入 SessionMapper/AgentConfigMapper/SessionManager/ToolManager。getOrCreate 通过 computeIfAbsent 线程安全加载 Session→AgentConfig→确定 modelId→通过 toolManager.getSessionTools() 获取 tools（提取 ToolSessionObject.toolConfig）→通过 sessionManager.getMessages() 获取 messages→转换 HistoryEntry→构建 AgentExecutionContext→封装 AgentSessionContext。remove 清除缓存。get 仅查询不创建。public record AgentSessionContext 包含 context/mutator/AtomicBoolean toolInvoking。
## ChatService.java

ChatService.toToolDefinition() 私有方法已移除，工具定义转换逻辑迁移至 ModelInvoker.toToolDefinition() 默认方法。chat() 方法中 invoker 变量提前声明，统一用于 toToolDefinition 和 invokeStream 调用。
chat() 方法 messages 构建时，对 HistoryEntry 中 toolCalls 非空且 reasoning 非空的条目，通过 builder.reasoning(entry.reasoning()) 将思考内容传入 Message DTO，确保 DeepSeek 等模型的 reasoning_content 能透传到 LLM API。
## MessageSavePostHook

已适配 chunk.getToolCalls() 返回 List，改为 for 循环遍历处理每个 ToolCallDelta。toolCallBuffers key Integer→String，getIndex→getId+兜底。累积逻辑（id/name/arguments 的 StringBuilder append）不变。
## ChatChunk DTO

toolCall 字段已改为 toolCalls (List&lt;ToolCallDelta&gt;)，支持多工具调用并行。新增 hasToolCalls Boolean 字段用于标记本轮对话是否出现工具调用。新增 index Integer 字段。
## ChatService 流管道工具调用跟踪

流管道中使用 AtomicBoolean 跟踪本轮是否出现 toolCall 块（已适配 toolCalls List 并检查非空非空列表），finishReason 块中设置 hasToolCalls 标记，doOnComplete 触发 AFTER_MESSAGE_RECEIVE HOOK 时传入含 hasToolCalls 的完整 chunk。支持 [tool_continue] 标记用于工具执行后的会话继续（跳过用户消息保存）。
## ToolCallBufferHook

重构 tool_calls 处理逻辑：遍历 chunk.getToolCalls()，以 tc.getId() 为 key（null 时跳过循环）；从 sb.toolCallBuffers 按 key 获取 ToolAccumulator；acc 为 null 或 acc.id 为空时新建并设置 id/name/arguments；acc.id 非空时仅追加 arguments。
## ToolExecutionController

executeTools() 和 toolStatus() 返回类型改为 ApiResponse&lt;Map&lt;String,Object&gt;&gt;，所有 Map 返回值使用 ApiResponse.success(map) 包装。
## ToolExecutionTracker

setFailed 修复：更新 currentExecutions status 为 failed 的同时将失败结果添加到 completedResults。新增 clear(sessionId) 清理方法。
## 内存泄漏防护

ChatService.doOnComplete 调用 toolCallBufferHook.clear(sessionId) 和 toolExecutionTracker.clear(sessionId)。SessionServiceImpl.deleteSession 同步清理两个组件数据。
## ChatService 常量

新增 public static final String TOOL_CONTINUE_MARKER = "[tool_continue]"，ToolExecutionController 引用此常量替代魔术字符串。
## 模型调用器

OpenAIInvoker.parseStreamChunk 方法中已移除 tool_calls 解析的 index 节点获取和设置逻辑，ToolCallDelta 构建时不再设置 index 字段。
ToolCallDelta.java 已移除 private Integer index 字段。
ToolCallBufferHook/MessageSavePostHook：toolCallBuffers key 类型 Integer→String，getIndex() 替换为 getId()（null 时兜底取最后一个 key）。AnthropicInvoker：移除 ToolCallDelta builder 的 .index(index) 调用。
## TypeScriptToolInvoker

TypeScriptToolInvoker 完全重写为 bun 运行时调用器：scriptPath 改为文件夹路径（构造时验证为目录），执行前检测 bun 和 node 环境可用性（不可用抛出 TOOL_RUNTIME_NOT_FOUND），首次执行时在文件夹下生成 `_runner.ts` 桥接文件，将 AgentExecutionContext（sessionId/agentId/systemPrompt/modelId/history/tools）和 arguments 序列化为 JSON（Long 字段转为 String 防止精度丢失），通过 ProcessBuilder("bun", runnerPath, json) 或 fallback "node" 执行桥接文件，捕获 stdout 作为返回结果，stderr 合并到输出流，超时 30 秒强制终止。
已创建 tools/file-reader/index.ts 文件读取测试工具，用于验证 TypeScriptToolInvoker 的 bun/node 运行时桥接机制。该工具定义与 _runner.ts 一致的接口（AgentExecutionContext/HistoryEntry/ToolCall/ToolInfo），导出 execute 函数接收 ctx 和 args JSON 参数，支持 path 和 encoding 参数，resolve 为绝对路径后 existsSync 校验存在性，readFileSync 读取内容，成功返回 JSON {path, size, content}，失败返回 JSON {error}。
fix: 修复 tools/file-reader/index.ts 审查问题：用 statSync(absolutePath).size 替代重复 readFileSync 获取文件大小，避免大文件读取两遍；函数参数 ctx 改为 _ctx 表示保留签名但未使用
fix: TypeScriptToolInvoker Windows 适配 — 新增 isWindows() 通过 os.name 检测 Windows；isCommandAvailable() Windows 下使用 cmd /c command --version 利用 cmd.exe 识别扩展名；resolveRuntime() Windows 下返回 cmd /c bun 或 cmd /c npx tsx 确保 CreateProcess 正确找到解释器
fix: JSON 参数改为 stdin 传递 — _runner.ts 新增 readStdin() 异步函数通过 for await (const chunk of process.stdin) 流式读取 stdin，main() 调用 readStdin() 替代 process.argv[2]；TypeScriptToolInvoker.java execute() 移除 command.add(jsonParams)，改为 process.getOutputStream().write() + close 写入 stdin，避免 Windows cmd /c 命令行 JSON 截断/转义问题
fix: JSON 参数改为临时文件传递 — 移除 process.getOutputStream().write() stdin 写入，改为将 jsonParams 写入 scriptDir 下 _input.json 临时文件，文件路径作为命令行参数传入 command.add(inputFile.toString())，在 finally 块中 Files.deleteIfExists(inputFile) 清理临时文件。修复 Windows cmd /c 下 stdin 管道"管道已结束"不可靠问题。
fix: 修复 tool_calls 参数丢失 — ToolCallDelta 新增 @Builder private Integer index 字段；OpenAIInvoker.parseStreamChunk 解析 tcNode.get("index") 设置 tcBuilder.index()；MessageSavePostHook id 为 null 时用 index fallback key 继续累加参数。修复 OpenAI 流 SSE 中 id 仅首chunk出现导致后续chunk参数被跳过的问题。
## ToolManager

ToolManager.java: Spring @Component，注入 ToolConfigService 和 SessionToolMapper。内置 ConcurrentHashMap 缓存 sessionToolCache（sessionId→List&lt;ToolSessionObject&gt;）和 toolCache（toolId→ToolSessionObject）。getInvoker(Long sessionId, String toolName) 从会话工具列表中查找对应 ToolInvoker。getSessionTools(Long sessionId) 从 sessionToolCache 获取，缓存空则查 SessionToolMapper 获取工具 ID，遍历 ID 从 toolCache 获取 ToolSessionObject，未命中则从 DB 查 ToolConfigDTO 并构建 invoker（JAVA→JavaToolInvoker / TYPESCRIPT→TypeScriptToolInvoker / PYTHON→PythonToolInvoker / MCP_HTTP→McpHttpToolInvoker），对 MCP_HTTP 类型自动调用 expandMcpTools() 运行时发现远程工具列表并展开。expandMcpTools() 为 private 方法，通过 McpJsonRpcClient 获取远程工具列表，构建 ToolConfigDTO 列表返回。失败时返回原配置列表作为兜底。ToolSessionObject 内部 record 包含 toolConfig、invoker、mcpOriginalConfig（非MCP为null）、mcpExpandedTools（非MCP为空列表）、mcpExpandedInvokers（非MCP为空列表）。execute(ToolInvoker, AgentExecutionContext, String) 封装调用器执行。
## SessionManager

SessionManager.java: Spring @Component，注入 MessageMapper 和 MessageToolCallMapper，提供 saveMessage（sequenceNum 自增+工具调用批量保存）、getMessages（按序返回含 toolCalls 的 MessageDTO 列表）两个方法。内部 record ToolCallData 和 MessageDTO 供 ToolExecutionController 等组件使用。getSessionTools() 和 expandMcpTools() 已迁移至 ToolManager。
## AgentExecutionContext.java

新增 sessionVariables 和 conversationVariables 两个 Map&lt;String, Object&gt; 字段（@Getter(AccessLevel.NONE) 不暴露公共 getter）；提供 putSessionVariable/getSessionVariable/putConversationVariable/getConversationVariable 四个方法进行读写。构造器接收两个 Map 参数由 AgentContextManager.getOrCreate() 传入空 HashMap 初始化。
