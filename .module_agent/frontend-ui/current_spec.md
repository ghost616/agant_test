前端 UI：模型管理、工具管理、HOOK 管理、智能体配置、对话交互界面
## 模型管理界面


- 模型配置管理页面 `/models`，支持模型列表展示、搜索筛选、新增/编辑/删除/启用禁用
- Table 列：名称、平台类型、模型名称、温度、最大Token、状态(Tag)、创建时间、操作(编辑/删除Switch)
- 筛选栏：名称搜索(Input.Search)、平台类型(Select)、状态(Select)
- 新增/编辑 Modal：name、platformType、apiKey(Input.Password)、baseUrl、modelName、temperature(InputNumber 0-2)、maxTokens(InputNumber)、description
- 删除使用 Popconfirm 确认
- 启用/禁用使用 Switch 切换
- API 服务封装：listModels、getModel、createModel、updateModel、deleteModel、updateModelStatus

- 新增 PlatformConfig 接口：{ platformType: PlatformType; defaultBaseUrl: string; modelNames: string[]; }，定义平台默认配置
- 新增 getPlatformConfig() API 函数，调用 GET /api/models/platform-config 获取平台配置列表
- 表单联动逻辑：
  - 组件挂载时自动获取平台配置
  - 通过 Form.useWatch 监听 platformType 字段变化
  - 新建模式下切换平台：自动填入对应 defaultBaseUrl 到 baseUrl 字段
  - 非 CUSTOM 新建：隐藏 baseUrl 表单项（自动填入值），CUSTOM 显示可手动输入
  - 编辑模式：baseUrl 始终显示（可编辑）
  - 非 CUSTOM 且 modelNames 非空：modelName 切换为 Select 下拉选择（支持搜索）
  - CUSTOM 或 modelNames 为空：modelName 保持 Input 自由输入
  - 新建时默认选中 OPENAI 平台并填入其 defaultBaseUrl
- 修复：AZURE/OLLAMA 平台新建模式下 Base URL 输入框被禁用的问题。将 disabled 条件从 `!isCustom && !editingModel` 改为 `!needsManualInput && !editingModel`，使所有需要手动输入 Base URL 的平台（AZURE/OLLAMA/CUSTOM）在新建模式下均可编辑。
- 模型列表操作列新增"测试"按钮，点击跳转 `/models/:id/test`
- 新增模型测试页面 `/models/:id/test`：加载模型信息，展示模型名称和平台标签，支持思考模式开关
- 对话交互：Input.TextArea 输入消息，Enter 发送（Shift+Enter 换行），流式接收 AI 回复
- 使用 react-markdown + remark-gfm 渲染回复内容（支持表格、代码块、列表等 GFM 语法），深色背景 (#1e1e1e) 展示区
- chatStream API：基于 fetch + ReadableStream 的 SSE 流式请求封装，支持 AbortController 取消
- 每次发送仅含当前消息，无上下文历史，清空按钮清除回复
- 温度(Temperature)和最大Token(MaxTokens)改为可选字段，不再要求必填，新建时不再预设默认值。后端在值为 null 时跳过传递，使用平台默认值。
- API `listModels` 不再返回分页结构，直接返回 `ModelConfig[]` 扁平数组，`ModelListParams` 移除 `page`/`size` 字段
- 模型列表表格移除分页器，全量展示所有符合条件的记录
- 修复编辑 Modal 打开时表单值丢失问题：将 `form.setFieldsValue` 从 `handleEdit` 移至 useEffect 监听 `editingModel` 和 `modalVisible` 变化后设置
- 操作列宽度从 200 调整为 260，适配测试按钮
- 模型测试页修复 React.StrictMode 导致 useEffect 重复调用 API 的问题：添加 calledRef 标记

- baseUrl 显示与禁用逻辑统一由 needsManualInput 决定（去掉 editingModel 条件）：showBaseUrl = needsManualInput，disabled = !needsManualInput，编辑模式与添加模式行为一致
- 修复 chatStream SSE 解析逻辑（src/services/model.ts：103-122行）：后端 invokeStream 输出纯 JSON 行（无 `data:` 前缀），移除 `line.startsWith('data: ')` 检查，改为 `!line.trim()` 跳过空行，直接 JSON.parse(line)；移除过时 `[DONE]` 检查，改为 `chunk.finishReason === 'stop'` 触发 onDone
- chatStream 方法添加调试日志：每个非空 SSE 行打印原始内容（`[chatStream] raw line:`），JSON 解析成功后打印 chunk 对象（`[chatStream] parsed chunk:`），解析失败时打印出错行和异常信息（`[chatStream] JSON parse failed for line:`）
- OpenAIInvoker.buildRequestBody 新增 thinking 属性处理：当 ChatRequest.thinking 为 true 时，向请求体添加 {"thinking": {"type": "enabled"}}，支持 DeepSeek 等平台的思考模式
- ChatChunk 接口新增 reasoning?: string 字段，支持接收服务端返回的推理/思考内容

- chatStream 接口新增 `onReasoning` 回调，在 SSE 解析循环中处理 `chunk.reasoning` 字段，支持接收服务端返回的推理/思考内容
- 模型测试页新增推理内容展示区域：接收并累积 reasoningText，以深色背景 (#252525) + 金色左侧边框 (#ffd700) 区分于普通回复
- 推理区域上方显示"思考过程"标签（金色小字），内容使用 ReactMarkdown 渲染
- 发送/清空操作同步清除 reasoningText 和 hasReasoningRef；onReasoning 回调中设置 hasResponseRef=true（只有 reasoning 无 delta 也算有内容）
## 工具管理界面

- 工具配置管理页面 `/tools`，支持工具列表展示、搜索筛选、新增/编辑/删除/启用禁用
- Table 列：名称、工具类型(JAVA/TYPESCRIPT/PYTHON Tag)、描述(ellipsis)、状态(Tag)、创建时间、操作(编辑/删除/Switch)
- 筛选栏：名称搜索(Input.Search)、工具类型(Select)、状态(Select)
- 新增/编辑 Modal：name(必填)、toolType(必填Select)、description(TextArea)、parameterSchema(TextArea JSON)、returnSchema(TextArea JSON)、implPath
- 删除使用 Popconfirm 确认
- 启用/禁用使用 Switch 切换
- API 服务封装：listTools、getTool、createTool、updateTool、deleteTool、updateToolStatus
- 工具名称表单项新增 pattern 校验规则：仅允许小写字母、数字和下划线（/^[a-z0-9_]+$/）
- 工具列表表格添加 pagination={false}，移除分页器，全量展示
- 新建 SchemaEditor 组件：结构化 JSON Schema 编辑器，解析 properties 为 PropertyDef 列表，每行编辑属性名/类型/描述/必填，构建回写 onChange
- ToolList.tsx parameterSchema Form.Item 替换 TextArea 为 SchemaEditor 组件
- 安装 @codemirror/view @codemirror/state @codemirror/lang-json @codemirror/basic-setup，提供 JSON 代码编辑器支持
- 新建 JsonEditor 组件（src/components/JsonEditor.tsx）：封装 CodeMirror EditorView + json() 语法高亮，value/onChange 兼容 Ant Design Form.Item，深色主题自适应高度
- ToolList.tsx：parameterSchema 和 returnSchema 的 TextArea/SchemaEditor 替换为 JsonEditor
- 删除 SchemaEditor.tsx（已被 JsonEditor 替代）
- 修复 CodeMirror 多实例冲突：卸载 @codemirror/basic-setup（v0.x 自带旧版 view/state 副本），安装 @codemirror/commands @codemirror/language
- JsonEditor.tsx 移除 basicSetup，改用独立扩展组装：lineNumbers/highlightActiveLineGutter/highlightSpecialChars/drawSelection @codemirror/view；defaultKeymap/history/historyKeymap @codemirror/commands；indentOnInput/bracketMatching/closeBrackets @codemirror/language
- 工具类型新增 MCP_HTTP：TOOL_TYPE_LABELS 添加 'MCP HTTP' 标签，TOOL_TYPE_COLORS 添加 'purple' 颜色
- MCP_HTTP 表单专项：通过 Form.useWatch('toolType') 监听，仅 MCP_HTTP 时显示 Authorization 输入框
- MCP_HTTP 类型隐藏 parameterSchema / returnSchema 表单项
- 提交逻辑：toolType === MCP_HTTP 时，取 authorization 值组装 authConfig: JSON.stringify({type: "bearer", token: authorization})，并清空 parameterSchema/returnSchema
- 编辑回填：编辑 MCP_HTTP 工具时从 authConfig JSON 解析 token 回填 authorization 表单字段
## 智能体管理界面

- 智能体配置管理页面 `/agents`，支持智能体列表展示、搜索筛选、新增/编辑/删除/启用禁用
- Table 列：名称、描述(ellipsis)、关联模型名称、状态(Tag)、创建时间、操作(编辑/删除/Switch)
- 筛选栏：名称搜索(Input.Search)、状态筛选(Select)
- 新增/编辑 Modal：name(必填)、description(TextArea)、systemPrompt(TextArea)、modelId(Select 从模型列表获取)、toolIds(Select multiple 从工具列表获取)
- 删除使用 Popconfirm 确认
- 启用/禁用使用 Switch 切换
- API 服务封装：listAgents、getAgent、createAgent、updateAgent、deleteAgent、updateAgentStatus
- 模型列表和工具列表数据通过 Promise.all 并行加载用于表单下拉选择
- 路由 /agents 注册，侧边栏"智能体管理"菜单项（RobotOutlined 图标）
- 智能体列表表格添加 pagination={false}，移除分页器，全量展示
- AgentConfig 与 AgentFormData 类型新增 recentMessageCount?: number 字段（最近消息数量）
- 新增/编辑 Modal 新增 Form.Item name="recentMessageCount" label="最近消息数量"：InputNumber，initialValue=10、min=1、max=100、宽度 100%
- 编辑回填时同步设置 recentMessageCount 字段
- Table columns 新增"最近消息"列（dataIndex=recentMessageCount，width 100），值为空时显示 '-'
## 会话管理界面

- 会话列表页面 `/sessions`，支持会话列表展示、新建会话、删除会话、继续会话
- Table 列：标题、智能体名称、模型名称、创建时间、操作（继续会话 Button + 删除 Popconfirm）
- 新建会话 Modal：选择智能体（Select，仅已启用）、选择模型（Select，仅已启用）、标题（可选 Input）
- 创建成功后自动跳转 /sessions/:id/chat
- 继续会话按钮跳转 /sessions/:id/chat
- 删除使用 Popconfirm 确认
- 智能体对话页面 `/sessions/:id/chat`：多轮对话，流式接收 AI 回复
- 复用模型测试页布局：深色背景消息展示区 (#1e1e1e)、Input.TextArea 输入、Enter 发送 Shift+Enter 换行
- 使用 react-markdown + remark-gfm 渲染回复，推理内容以金色边框区域展示
- agentChatStream API：基于 fetch + ReadableStream 的 SSE 流式封装，调用 POST /api/chat，回调 onDelta/onReasoning/onDone/onError
- 路由 /sessions 和 /sessions/:id/chat 已注册，侧边栏"会话管理"菜单项（MessageOutlined 图标）
- API 服务封装：listSessions、createSession、deleteSession、agentChatStream
- 对话页面加载时从后端 GET /api/sessions/${sessionId}/messages 获取历史消息并展示
- 历史消息加载中显示 Spin 提示
- 消息角色区分布局：user 消息居右（flex-end），assistant/tool/system 消息居左（flex-start）
- 每种角色有专属图标和颜色标签：user(UserOutlined/#569cd6)、assistant(RobotOutlined/#4ec9b0)、tool(ToolOutlined/#d7ba7d)、system(InfoCircleOutlined/#9cdcfe)
- 消息气泡样式：user 蓝色系(#1a3a5c)、assistant 深色(#2a2a2a)、tool 灰色(#3a3a3a)、system 深蓝(#2d3748)，最大宽度 75%
- ChatMessage 接口 role 扩展为 'user' | 'assistant' | 'tool' | 'system'
- 流式回复使用相同的居左+图标布局，保留推理内容金色边框样式
- 新增 SessionMessage/ToolCallData 类型定义、getSessionMessages API 函数
- 历史消息加载使用 calledRef 防止 StrictMode 重复调用
- BUBBLE_STYLES 常量移除所有角色的 maxWidth 属性，避免与外层 wrapper 的 maxWidth: '75%' 双层叠加导致短消息气泡被额外压缩、文字异常换行

- agentChatStream API 的 ChatChunk 接口新增 hasToolCalls 布尔字段
- onDone 回调签名改为 `(hasToolCalls: boolean) => void`
- SSE 解析循环中 `finishReason === 'stop'` 时将 `chunk.hasToolCalls` 传递给 onDone
- 新增 executeTools(sessionId) API：POST /api/chat/{sessionId}/execute-tools，返回 { status, toolId, toolName, arguments, hasMore }
- 新增 getToolStatus(sessionId) API：GET /api/chat/{sessionId}/tool-status，返回 { status, toolId, toolName, arguments, result?, hasMore? }
- 新增 continueChatStream(sessionId, callbacks) API：POST /api/chat/{sessionId}/continue，返回 SSE 流，callbacks 结构与 agentChatStream 一致（含 hasToolCalls）
- 对话页面集成工具调用流程：onDone 检测 hasToolCalls→executeTools 获取工具→轮询 getToolStatus（1s 间隔）→done 时以 role="tool" 气泡展示调用信息与结果→hasMore 继续下一工具→全部完成调用 continueChatStream 流式续接
- 工具执行期间显示独立加载状态（"正在执行工具调用..."），区别于普通 SSE 流等待
- 支持 AbortController 中止工具执行循环（toolAbortRef）

- agentChatStream 与 continueChatStream 共同的 SSE 解析逻辑抽取为 processSSEStream 内部函数
- ExecuteToolsResult 接口中 toolId/toolName/arguments 标记为可选字段
- executeToolLoop 中 executeTools 返回 status='empty' 时跳过工具消息和 continueChat（直接终止等待）
- pollToolStatus 移除 status==='completed' 死代码分支，新增 status==='idle' 分支继续轮询
- continueChatStream 的 onDone 递归调用加 MAX_TOOL_LOOPS=10 保护，超过上限终止并提示
- 每次新发送消息或工具流程正常结束时重置 toolLoopCount
- AgentChat.tsx 流式数据为空不显示：handleSend 和 executeToolLoop 中 onDone 回调添加 content 非空检查（prev && prev.trim()），为空则跳过不添加 assistant 消息
- 工具执行气泡补充 arguments 参数显示：工具开始执行气泡 content 加入 JSON 格式参数；工具完成/失败气泡同样补充参数显示
- SessionMessage 接口新增 toolName 可选字段，历史消息加载映射时保留该字段
- tool 角色消息渲染时在角色头下方显示工具名称（toolName），为空时显示空字符串
- loadHistory 中历史消息映射：当 msg.role === 'tool' 且 msg.toolName 非空时，将 "**工具: {toolName}**\n\n" 拼接到 msg.content 前面，使历史工具消息的 toolName 像流式消息一样嵌入 content 中显示
- SessionMessage 接口：toolName 字段替换为 toolResult（String，可选），存储工具执行结果的 JSON
- ChatMessage 接口：toolName 替换为 toolResult
- loadHistory 历史消息映射：tool 角色且 toolResult 非空时，解析 toolResult JSON 获取 toolName/arguments/result，格式化为完整工具执行结果 markdown 作为 content（替代之前仅拼接 toolName 前缀的逻辑）