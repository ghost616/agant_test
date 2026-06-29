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
新增 LoadSkillsSystemTool（load_skills，按名称从 ctx.getSkills() 匹配可用技能，读写 _sys_loading_SKILLS 会话变量实现去重合并加载）和 UnloadSkillsSystemTool（unload_skills，从 _sys_loading_SKILLS 移除指定名称并写回）。两者均为 @Component，由 SystemToolManager 自动注册。
## 实体定义

Message 实体对应 message 表，不继承 BaseEntity（无 update_time/deleted 列）。字段：id (雪花ID)、sessionId、role、content、reasoning、sequenceNum、toolCallId、toolResult (String, @TableField("tool_result"))、createTime。
## AgentContextManager

loadSessionVariables() 返回类型改为 Map&lt;String,String&gt;；injectVariableCallbacks() 回调中移除 String.valueOf() 包装（value 已确认是 String）。
getOrCreate() 移除 AgentContextMutator 参数，方法内部自行 new AgentContextMutator()。
getOrCreate() 从 AgentConfig 获取 recentMessageCount 传入 AgentExecutionContext 构造器。
getOrCreate() 新增 skills 加载逻辑：buildSkills() 私有方法通过 AgentSkillMapper→SkillConfigMapper→SkillToolMapper→ToolConfigMapper 四级链路查询技能配置及关联工具列表，仅加载 ENABLED 状态的技能，构建 SkillConfigDTO 列表传入 AgentExecutionContext 构造器。
## ChatService.java

ChatService.toToolDefinition() 私有方法已移除，工具定义转换逻辑迁移至 ModelInvoker.toToolDefinition() 默认方法。chat() 方法中 invoker 变量提前声明，统一用于 toToolDefinition 和 invokeStream 调用。
chat() 方法 messages 构建时，对 HistoryEntry 中 toolCalls 非空且 reasoning 非空的条目，通过 builder.reasoning(entry.reasoning()) 将思考内容传入 Message DTO，确保 DeepSeek 等模型的 reasoning_content 能透传到 LLM API。
chat() 移除外部 new AgentContextMutator() 和 getOrCreate mutator 参数传入；非工具继续路径（!isToolContinue）调用 contextMutator.clearConversationVariables() 清除上一轮对话变量。
chat() 构建 messages 时根据 context.getRecentMessageCount() 按配对截取 history 最近 N 条。新增 truncateByPairs() 私有方法：按 user 消息划分配对，取尾部 pairCount 个配对的子列表。recentMessageCount 为 null 或 <=0 时保持全部历史。
fix: 修复编译错误 — foldMessageGroups() 中第 214 行将 record 风格 messages.get(i).role() 修正为 Lombok @Data 生成的 getter messages.get(i).getRole()（Message DTO 为 @Data 类，访问角色须用 getRole()）。
chat() 新增 SKILL 注入逻辑：(1) 系统提示词后追加技能列表 system 消息（列出可用技能名称和描述，告知 _sys_load_skills/_sys_unload_skills 系统工具使用方法），(2) 解析 _sys_loading_SKILLS 会话变量获取已加载技能并追加提示词 system 消息，(3) 工具列表用 LinkedHashMap 按 name 去重合并已加载技能的 skillTools（经 invoker.toToolDefinition 转换）。新增 parseLoadedSkills() 私有方法解析 JSON 数组匹配技能配置。
## MessageSavePostHook

已适配 chunk.getToolCalls() 返回 List，改为 for 循环遍历处理每个 ToolCallDelta。toolCallBuffers key Integer→String，getIndex→getId+兜底。累积逻辑（id/name/arguments 的 StringBuilder append）不变。
## ChatChunk DTO

toolCall 字段已改为 toolCalls (List&lt;ToolCallDelta&gt;)，支持多工具调用并行。新增 hasToolCalls Boolean 字段用于标记本轮对话是否出现工具调用。新增 index Integer 字段。
## ChatService 流管道工具调用跟踪

流管道中使用 AtomicBoolean 跟踪本轮是否出现 toolCall 块（已适配 toolCalls List 并检查非空非空列表），finishReason 块中设置 hasToolCalls 标记，doOnComplete 触发 AFTER_MESSAGE_RECEIVE HOOK 时传入含 hasToolCalls 的完整 chunk。支持 [tool_continue] 标记用于工具执行后的会话继续（跳过用户消息保存）。
## ToolCallBufferHook

重构 tool_calls 处理逻辑：遍历 chunk.getToolCalls()，以 tc.getId() 为 key（null 时跳过循环）；从 sb.toolCallBuffers 按 key 获取 ToolAccumulator；acc 为 null 或 acc.id 为空时新建并设置 id/name/arguments；acc.id 非空时仅追加 arguments。
## ToolExecutionController

continueChat() 保存 tool 消息时构建 toolResult JSON 字符串（{"toolName":"xxx","arguments":"xxx","result":"xxx"}），传入 saveMessage 的 toolResult 参数，toolCallId 传 null。已注入 ObjectMapper 用于序列化。
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

saveMessage() 方法的 toolName 参数已替换为 toolResult (String)，内部调用 message.setToolResult()。MessageDTO record 中 toolName 字段替换为 toolResult。getMessages() 构造 MessageDTO 时传入 msg.getToolResult()。
## AgentExecutionContext.java

Map&lt;String,Object&gt; 改为 Map&lt;String,String&gt;，所有 putSessionVariable/getSessionVariable/putConversationVariable/getConversationVariable 方法签名以及 AgentContextMutator.BiConsumer 回调参数类型同步改为 String。
新增 getSessionVariableKeys() 和 getConversationVariableKeys() 返回 Set&lt;String&gt;，供 ContextSerializer 遍历变量用于 JSON 序列化。
AgentContextMutator 新增 clearConversationVariables() 方法，清除绑定 context 的 conversationVariables Map。
构造器新增 recentMessageCount(Integer) 参数，存储为字段并通过 @Getter 生成 getRecentMessageCount()。recentMessageCount 为 null 或 <=0 时表示不截取历史。
构造器新增 skills 字段（List&lt;SkillConfigDTO&gt;）及 Lombok @Getter，skills 为只读不可变列表，供工具执行时查看智能体关联的 SKILL 配置。
## ContextSerializer.java

serializeToJson() 新增 sessionVariables 和 conversationVariables 序列化：通过 ctx.getSessionVariableKeys()/getConversationVariableKeys() 遍历，逐个 key-value 写入 ObjectNode 并挂到 contextJSON 上。
serializeToJson() 新增 recentMessageCount JSON 序列化（ctx.getRecentMessageCount()），插入在 modelId 之后
serializeToJson() 新增 skills 序列化：遍历 ctx.getSkills()，每个 skill 输出 name/description/prompt，skillTools 子数组输出 name/description/parameterSchema。
## _runner.ts 模板

新增 VariableChanges 接口和 createVariableProxy() 工具函数：对 sessionVariables/conversationVariables 创建 Proxy 拦截 set/delete 操作，执行后 diff 对比原始快照生成变更集 {added:{},removed:[]}，输出结构化 JSON {result,sessionVariables,conversationVariables}，兼容旧格式降级。
AgentExecutionContext 接口新增 recentMessageCount?: number 可选字段
新增 SkillInfo 接口（name/description/prompt/skillTools: ToolInfo[]），AgentExecutionContext 接口新增 skills?: SkillInfo[]。
## TypeScriptToolInvoker.java

新增 parseResult() 私有方法：解析结构化 JSON 输出 {result,sessionVariables,conversationVariables}，遍历 added/removed 调用 ctx.putSessionVariable/removeSessionVariable 和 ctx.putConversationVariable/removeConversationVariable 同步变量到 Java 上下文；非结构化 JSON 降级按纯文本返回。execute() 返回改为 parseResult(ctx,result)。
## _runner.py 模板

新增 VariableProxy 类（dict 代理包装）：重写 `__setitem__`/`__delitem__` 捕获写入和删除操作，`get_changes()` 返回变更集 `{added:{},removed:[]}`。AgentExecutionContext 读取 sessionVariables/conversationVariables 到 session_variables/conversation_variables 属性。main() 执行前 Proxy 替换、执行后 diff 输出结构化 JSON `{result,sessionVariables,conversationVariables}`。
新增 SkillInfo 类（解析 name/description/prompt/skill_tools），skill_tools 解析为 ToolInfo 列表。AgentExecutionContext 解析中新增 self.skills 字段。
## PythonToolInvoker.java

新增 parseResult() 私有方法：解析结构化 JSON 输出 `{result,sessionVariables,conversationVariables}`，遍历 added/removed 调用 ctx.putSessionVariable/removeSessionVariable 和 ctx.putConversationVariable/removeConversationVariable 同步变量到 Java 上下文；非结构化 JSON 降级按纯文本返回。execute() 返回改为 parseResult(ctx,result)。
## 工具测试

新增 session-var-write-py / session-var-read-py / session-var-write-ts / session-var-read-ts 四个 session 变量读写验证工具。
