前端 UI：模型管理、工具管理、HOOK 管理、智能体配置、对话交互界面
## 模型管理界面

- baseUrl 显示由 isCustom 决定（仅"自定义"CUSTOM 平台显示 Base URL 输入框），其余平台隐藏并自动填充 defaultBaseUrl；disabled 由 needsManualInput 决定（编辑模式与添加模式行为一致）：showBaseUrl = isCustom，disabled = !needsManualInput

- 模型测试页面按 modelType 分支：LLM 显示对话测试界面（chatStream 流式对话、思考模式、推理展示），EMBEDDINGS 显示嵌入测试界面（输入限制 1000 字符超出报错、embed API 调用、结果区展示前 100 维向量超出显示 '...'）；页面头部显示 modelType 标签
- types/model.ts 提供 EmbeddingRequest/EmbeddingResponse 类型；services/model.ts 提供 embed(id, request) 调用 POST /api/models/{id}/embed

- EmbeddingResponse 与后端 DTO 一致：{ embeddings: EmbeddingItem[]; usage?: UsageInfo }，EmbeddingItem 为 { index: number; embedding: number[] }；EmbeddingTest 组件取 result.embeddings[0].embedding 展示，embeddings 为空或缺失时显示占位文本"未返回嵌入向量"
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
- 新增/编辑 Modal 新增 skills 多选：通过 Promise.all 并行加载模型/工具/技能列表，新增 skillList state 存储技能数据，fetchModelsAndTools 改名为 fetchRefData 同步加载三种引用数据，表单中添加 skillIds (Select multiple) 字段实现技能多选，编辑回填时同步设置 skillIds
- AgentConfig/AgentFormData 中 toolIds/skillIds 改为 tools/skills（数组，每项含 id + sessionAuth 字段）
- SessionAuthType：ALL（所有会话）/ PARENT（父会话）/ CHILD（子会话）
- 表单工具/技能选择使用 SessionAuthSelect 组件：多选 + 每项可配置 sessionAuth 下拉（默认 ALL）
- 表格列中 tools/skills 显示为 Tag 标签，颜色区分 sessionAuth 类型（blue/green/orange）
- 智能体绑定知识库：AgentConfig/AgentFormData 新增 knowledgeBaseIds?: string[] 字段，新增 KnowledgeBaseItem 类型（{ knowledgeBaseId: string; name: string }）；fetchRefData 并行加载知识库列表（listKnowledgeBases({})）构建 knowledgeBaseList 与 knowledgeBaseMap；表单新增"绑定知识库"多选 Select（mode=multiple，从知识库列表获取）；表格新增"绑定知识库"列（knowledgeBaseMap 映射 ID→名称渲染 Tag 列表，空显示 '-'）；createAgent/updateAgent 复用透传 knowledgeBaseIds
- 智能体向量模型：AgentConfig 与 AgentFormData 新增 vectorModelId?: string 字段；AgentList fetchRefData 并行加载 listModels({modelType:'EMBEDDINGS'}) 构建 vectorModelList，编辑回填 vectorModelId，表单新增"向量模型"下拉（仅 memoryEnabled=true 时显示，hidden={!memoryEnabled}），提交时 memoryEnabled 为 false 则 vectorModelId 置 undefined
- 记忆功能表单联动：memoryEnabled=false 时"保留记忆数量"（memoryGroupCount）与"向量模型"（vectorModelId）表单项均隐藏（hidden={!memoryEnabled}），memoryEnabled=true 时显示；提交时 memoryEnabled 为 false 则 vectorModelId 置 undefined
## 会话管理界面

- Web 搜索结果显示：ChatChunk 新增 webSearchCall 字段（WebSearchCall[] 数组，每项 { itemId, outputIndex, results: [{ title, url, snippet }] }）和 customToolCall 字段，StreamCallbacks 新增 onWebSearchCall 回调（(calls: WebSearchCall[]) => void），processSSEStream 解析 chunk.webSearchCall 数组并回调
- SessionMessage 新增 webSearchCall 可选字段（WebSearchCall[] 数组），存储持久化的搜索结果引用
- AgentChat 实时对话：handleSend 与 executeToolLoop continueChatStream 的流回调接收 onWebSearchCall，通过 currentWebSearchCall 数组状态在消息区域遍历展示多个搜索结果引用（标题链接+摘要），onDone 时将 webSearchCall 数组附加到 assistant 消息
- AgentChat 历史消息加载：loadHistory/loadChildMessages 从 SessionMessage.webSearchCall 数组映射渲染已持久化的多个搜索结果引用
- 对话 conversationId 支持：ChatRequest 类型（src/types/session.ts）新增 conversationId?: string 可选字段（对应后端 ChatRequest DTO）；services/session.ts 新增 fetchConversationId() 调用 GET /conversation-id 返回 conversationId，agentChatStream 参数类型改为 ChatRequest；AgentChat handleSend 每次用户发送前先 await fetchConversationId() 获取新 conversationId 传入 agentChatStream，获取失败时 message.error 并中止发送；工具续接 continueChatStream（[tool_continue]）请求不传 conversationId
- 会话历史功能：SessionMessage 接口新增 conversationId?: string 可选字段；services/session.ts 新增 getConversationMessages(conversationId) 调用 GET /api/conversations/{conversationId}/messages 返回对话消息列表；新建 ConversationHistory.tsx（路由 /conversations 展示主会话列表复用 listSessions，点击行跳转 /conversations/:sessionId；该页基于路由参数 sessionId 用 getSessionMessages 拉取并按 role==='user' 过滤展示用户消息列表，每条显示内容/时间，有 conversationId 的显示「查看详情」按钮跳转 /conversations/:conversationId/detail）；新建 ConversationDetail.tsx（路由 /conversations/:conversationId/detail，调用 getConversationMessages 展示该 conversationId 下所有消息，列：角色 Tag/内容/时间）；App.tsx 新增「会话历史」菜单项（HistoryOutlined）与 /conversations、/conversations/:sessionId、/conversations/:conversationId/detail 路由
- 对话详情页增强（ConversationDetail.tsx）：内容列对 assistant 且有 toolCalls 的消息显示「查看工具 (N)」按钮，对 tool 角色消息显示「查看结果」按钮，点击弹出 Modal 以 <pre> 展示 JSON.stringify(toolCalls/toolResult, null, 2)；新增「来源会话」列展示 sessionId（Tooltip 悬浮完整 ID，可见截短为前8后4…）；Table 使用 rowClassName 按 record.sessionId===sessionId 区分主/子会话背景色（conversation-main-row 暖黄 / conversation-child-row 浅蓝，样式定义在 index.css）
- 对话详情页内容列重构（ConversationDetail.tsx）：内容列改为可点击纵向三行展示（单行省略，LINE_ROW_STYLE nowrap/ellipsis）——💭 reasoning（有则）、📝 content（有则）、操作按钮（assistant 有 toolCalls 显示「🔧 工具调用 (N)」，tool 角色显示「📋 工具结果」）；点击内容区域（onClick setDetailVisible(true)）打开「对话详情」Modal 展示完整对话流（renderMessageFlow）：user 消息显示 content，assistant 消息显示 💭reasoning/📝content/🔧工具调用列表（renderToolCallFlow 展示 each 工具名称+参数 JSON，并按 toolCallId 通过 findToolResult 遍历后续 tool 消息配对展示该工具的 📋结果 JSON），tool 消息显示 toolName 与 result JSON；「来源会话」列与 rowClassName 主/子会话背景色逻辑保留
- App.tsx MENU_ITEMS：「会话历史」（HistoryOutlined）菜单位置从评估管理之前调整到评估管理之后，路由不变
## 技能管理界面

- 技能配置管理页面 `/skills`，支持技能列表展示、搜索筛选、新增/编辑/删除/启用禁用
- Table 列：名称、描述(ellipsis)、状态(Tag green/red)、创建时间、操作(编辑/删除Popconfirm/Switch)
- 筛选栏：名称搜索(Input.Search)、状态筛选(Select)
- 新增/编辑 Modal：name(必填)、description、prompt(必填 TextArea rows=6)、toolIds(Select multiple 从工具列表获取)
- 删除使用 Popconfirm 确认
- 启用/禁用使用 Switch 切换
- API 服务封装：listSkills、getSkill、createSkill、updateSkill、deleteSkill、updateSkillStatus
- 工具列表数据用于表单 toolIds 下拉选择
- 路由 /skills 已注册，侧边栏"技能管理"菜单项（ThunderboltOutlined 图标）
- Table pagination={false} 全量展示
## 评估管理界面

- 智能体评估配置管理页面 `/evaluations`，支持评估列表展示、新增/编辑/删除/启用禁用
- Table 列：名称、描述、智能体名称、状态(Tag green/red)、创建时间、操作(编辑/进行评估/禁用/删除Popconfirm)
- Table pagination={false} 全量展示
- 新增/编辑 Modal：name(Input 必填)、description(TextArea)、agentId(Select 从智能体列表获取)
- 删除使用 Popconfirm 确认
- 禁用使用 Switch 切换
- 「进行评估」按钮跳转 `/evaluations/{id}/items`（进入该智能体评估的评估项列表）
- 评估项列表页面 `/evaluations/:agentEvalId/items`：从路由参数获取 agentEvalId
- Table 列：名称、描述、智能体名称、执行次数、模型ID、创建时间、操作(编辑/进行评估/查看结果/删除Popconfirm)
- 新增/编辑 Modal：name、description、agentEvalId(只读显示)、modelId(Select)、executionCount(InputNumber)
- 返回按钮导航到 `/evaluations`
- 评估结果历史列表页面 `/evaluations/items/:evaluationId/results`：展示指定评估的执行结果历史
- 页面标题显示评估名称，上方有「执行」按钮（暂无功能，点击提示"功能开发中"）
- 返回按钮根据评估的 agentEvalId 导航回 `/evaluations/:agentEvalId/items`
- Table 列：ID、会话ID、Token消耗、结果摘要、创建时间、操作(修改按钮暂无功能)
- API 服务封装：
  - agentEvaluation.ts: getAgentEvaluationList、getAgentEvaluation、createAgentEvaluation、updateAgentEvaluation、deleteAgentEvaluation、updateAgentEvaluationStatus
  - evaluation.ts: getEvaluationList(支持 agentEvalId 筛选)、getEvaluation、createEvaluation、updateEvaluation、deleteEvaluation、getEvaluationResults
- EvaluationList 操作列新增"清空结果"按钮（danger 类型）：Modal.confirm 二次确认后调用 clearEvaluationResults(record.id) 清空该评估下所有评估结果，成功后 message.success 并刷新列表，失败 message.error；按钮始终可点击（后端对空列表无操作处理）
- useEvaluationExecute FOREGROUND 模式：sendForegroundMessage 在调用 agentChatStream 前先 await fetchConversationId() 获取 conversationId，获取失败时抛出错误中止；conversationId 传入 agentChatStream 请求参数（{ sessionId, content, conversationId }）
- useEvaluationExecute BACKGROUND 模式为 status→stream(cacheId)→remove(cacheId)→status 循环：executeEvaluation 返回 ExecutionStatusResponse（含 executionSessionId），拿到 executionSessionId 后先调 getEvaluationCacheStatus（GET /evaluations/cache/status?sessionId=xxx）获取 CacheStatusResponse（hasCache+cacheId），hasCache 为 true 且存在 cacheId 时用 getEvaluationStream 连接 GET /api/evaluations/cache/{cacheId}/stream（SSE，复用 processSSEStream），onDelta/onReasoning 增量追加到前台日志显示区域；流结束后调用 removeEvaluationCache（DELETE /evaluations/cache/{cacheId}）清理缓存，再回到 status 循环直到 hasCache=false；执行完成后刷新结果列表
- evaluation.ts 服务层：executeEvaluation 返回 ExecutionStatusResponse；getEvaluationStream 连接 GET /api/evaluations/cache/{cacheId}/stream（fetch + processSSEStream，返回 AbortController）；getEvaluationCacheStatus 调用 GET /evaluations/cache/status?sessionId=xxx 返回 CacheStatusResponse { hasCache, cacheId? }；removeEvaluationCache 调用 DELETE /evaluations/cache/{cacheId}（三个缓存接口均对齐后端 EvaluationExecutionController @RequestMapping(/api/evaluations) 前缀）；types/evaluation.ts 的 CacheStatusResponse 含 hasCache 与 cacheId 可选字段，ExecutionStatusResponse 含 evaluationId/executionSessionId 可选字段
- session.ts 导出 processSSEStream/StreamCallbacks/ChatChunk 供 evaluation.ts 复用
- EvaluationResultList 执行日志 Modal 标题由「前台执行」改为「执行日志」（BACKGROUND/FOREGROUND 共用）
- evaluation.ts 服务层移除 getExecutionStatus（GET /evaluations/{id}/execute/status）与 useEvaluationExecute 中 pollExecutionStatus/getExecutionStatus 轮询逻辑；executeEvaluation（POST /evaluations/{id}/execute）返回后直接进入 status→stream→remove 循环；types/evaluation.ts 的 ExecutionStatusResponse 移除 status/currentStep/totalSteps 字段，仅保留 evaluationId/executionSessionId
## 知识库管理界面

- 知识库管理页面 `/knowledge`：列表展示、名称搜索、状态筛选、新增/编辑/删除/启用禁用，"管理文件"跳转 `/knowledge/:kbId/files`
- 知识文件列表页面 `/knowledge/:kbId/files`：按路由参数 kbId 加载，新增/编辑/删除/启用禁用、发布文件占位按钮；新建/编辑弹窗仅 fileName/fileDescription（不含 fileContent）
- 知识文件内容编辑页面 `/knowledge/:kbId/files/:fileId/edit`：并行调用 getKnowledgeFile 加载文件元信息（文件名）与 getKnowledgeFileContent 加载内容，左右分栏（左 TextArea 编辑 Markdown，右 react-markdown + remark-gfm 实时预览，左右使用相同高度设置），底部右下角（flex-end justify）提供"保存"（调用 updateKnowledgeFileContent）与"关闭"按钮，无返回按钮
- API 服务封装：知识库/知识文件 CRUD + 状态切换 + 内容专用接口（getKnowledgeFileContent/updateKnowledgeFileContent，路径 /knowledge-bases/{kbId}/files/{id}/content），路径基于 /knowledge-bases 与 /knowledge-bases/{kbId}/files
- updateKnowledgeFileContent 请求体为原始字符串并覆盖 Content-Type: text/plain（与后端 consumes 对齐，避免 415）；getKnowledgeFileContent 返回 ApiResponse.data 内容字符串
- KnowledgeFile 类型不含 fileContent 字段，KFFormData 仅 fileName/fileDescription，文件内容通过内容专用接口单独读写
- 知识文件发布功能：types/knowledge.ts 新增 PublishStatus 类型（UNPUBLISHED/PUBLISHING/PUBLISHED/PENDING_PUBLISH/PUBLISH_ERROR）；KnowledgeFile 新增 publishStatus 字段；KnowledgeBase 新增 vectorModelId/esIndex/rebuilding 字段；KBFormData 排除 rebuilding
- services/knowledge.ts 新增三个接口：publishKnowledgeFile(kbId, fileId) POST /knowledge-bases/{kbId}/files/{fileId}/publish、refreshKnowledgeFiles(kbId) PUT /knowledge-bases/{kbId}/files/refresh、rebuildKnowledgeBaseES(kbId) POST /knowledge-bases/{kbId}/rebuild-es
- KnowledgeBaseList：操作列新增「ES数据重构」按钮（调用 rebuildKnowledgeBaseES，执行中 loading，rebuilding=true 时禁用）；知识库 rebuilding=true 时「管理文件」按钮禁用置灰；编辑弹窗新增向量模型下拉（listModels({modelType:'EMBEDDINGS'}) 加载 EMBEDDINGS 模型）与 ES 索引输入框，提交时空字符串归一化为 undefined
- KnowledgeFileList：发布状态 Tag 列（UNPUBLISHED=default 灰、PUBLISHING=processing 蓝、PUBLISHED=success 绿、PENDING_PUBLISH=warning 橙、PUBLISH_ERROR=error 红）；「发布」按钮仅 UNPUBLISHED/PENDING_PUBLISH/PUBLISH_ERROR 可用，publishStatus=PUBLISHING 时显示「发布中」并 disabled，点击调用 publishKnowledgeFile；知识库 rebuilding=true 时禁用发布按钮；新增「刷新」按钮调用 refreshKnowledgeFiles 后重新拉取列表；页面加载 getKnowledgeBase 获取 rebuilding 状态
- KnowledgeFileEdit：文件 publishStatus=PUBLISHING 时禁用 TextArea 与保存按钮，文件名旁显示「发布中，暂不可编辑」Tag
## 日志查看界面

- 运行日志页面 /logs：筛选栏含会话名搜索(Input.Search)、日志类型 Select、日志等级 Select，变更后重置到第 1 页
- Table 列：会话名(sessionName)、对话ID(conversationId)、日志类型(中文 Tag)、日志等级(彩色 Badge: INFO=blue/ERROR=red)、日志数据(超 60 字符截断+展开按钮)、创建时间
- 分页支持每页条数切换(20/50/100)，默认按创建时间倒序（后端排序），showTotal 展示总条数
- 日志详情 Modal：展示会话名/对话ID/日志类型/日志等级/创建时间元信息 + 完整日志数据(<pre> JSON 美化)
- types/log.ts 提供 AgentLog 类型、AgentLogQueryParams 查询参数、LogType/LogLevel 常量枚举（code 对齐后端枚举，label 中文）；LogType 不含 CALL_SOURCE，LogLevel 仅含 INFO/ERROR（不含 WARN）
- AgentLogList 中 LOG_LEVEL_LABELS/LOG_LEVEL_OPTIONS 由 LogLevel 枚举自动生成，LOG_LEVEL_COLORS 仅映射 INFO/ERROR（未知等级回退 default 颜色与原始值）
- services/log.ts 提供 listAgentLogs(params) 调用 GET /api/agent-logs 返回 PageResult<AgentLog>
- src/types/__tests__/log.test.ts 提供 LogType/LogLevel 枚举结构静态测试（不含 CALL_SOURCE/WARN、仅 INFO/ERROR、code/label 非空）
## 记忆回看界面

- 记忆回看会话列表页面 `/memory`：调用 listSessions 与 listAgents 并行加载，过滤 memoryEnabled=true 智能体会话（agentMap[session.agentId]?.memoryEnabled === true），表格列：会话名称（title，ellipsis）、智能体名（agentMap 映射）、最近消息时间（getSessionMessages 取末条消息 createTime，为空显示 '-'）；每条操作提供「按日聚合」「按分类聚合」两个按钮，分别跳转 `/memory/:sessionId/DAILY` 与 `/memory/:sessionId/GROUP`；Table pagination={false}
- 记忆聚合列表页面 `/memory/:sessionId/:type`：根据 type 参数调用 getSessionMemory（GET /api/sessions/{id}/memory，params type/page/size），返回 PageResult<SessionMemoryDocument>；DAILY 类型显示「聚合日期」列（aggregationStartTime 毫秒时间戳格式化为日期），GROUP 类型显示「起始-结束」列（aggregationStartSeq - aggregationEndSeq）；「聚合文本」列 ellipsis 超长省略；支持分页（pageSizeOptions 10/20/50，showTotal，页码变化重置逻辑）；页面标题按 type 显示「按日聚合记忆」/「按分类聚合记忆」；返回按钮跳转 /memory
- types/memory.ts 提供 MemoryAggregationType（GROUP/DAILY）、SessionMemoryDocument（sessionId/aggregationType/aggregationStartSeq/aggregationEndSeq/aggregationStartTime/aggregationEndTime/aggregationText/vector，与后端 SessionMemoryDocument 对齐）、MemoryQueryParams 类型
- services/memory.ts 提供 getSessionMemory(sessionId, type, page, size) 调用 GET /api/sessions/{id}/memory 返回分页结果
- App.tsx 新增「记忆回看」菜单项（EyeOutlined）与 /memory、/memory/:sessionId/:type 路由