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
## 会话管理界面

- Web 搜索结果显示：ChatChunk 新增 webSearchCall 字段（WebSearchCall[] 数组，每项 { itemId, outputIndex, results: [{ title, url, snippet }] }）和 customToolCall 字段，StreamCallbacks 新增 onWebSearchCall 回调（(calls: WebSearchCall[]) => void），processSSEStream 解析 chunk.webSearchCall 数组并回调
- SessionMessage 新增 webSearchCall 可选字段（WebSearchCall[] 数组），存储持久化的搜索结果引用
- AgentChat 实时对话：handleSend 与 executeToolLoop continueChatStream 的流回调接收 onWebSearchCall，通过 currentWebSearchCall 数组状态在消息区域遍历展示多个搜索结果引用（标题链接+摘要），onDone 时将 webSearchCall 数组附加到 assistant 消息
- AgentChat 历史消息加载：loadHistory/loadChildMessages 从 SessionMessage.webSearchCall 数组映射渲染已持久化的多个搜索结果引用
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
## 知识库管理界面

- 知识库管理页面 `/knowledge`：列表展示、名称搜索、状态筛选、新增/编辑/删除/启用禁用，"管理文件"跳转 `/knowledge/:kbId/files`
- 知识文件列表页面 `/knowledge/:kbId/files`：按路由参数 kbId 加载，新增/编辑/删除/启用禁用、发布文件占位按钮；新建/编辑弹窗仅 fileName/fileDescription（不含 fileContent）
- 知识文件内容编辑页面 `/knowledge/:kbId/files/:fileId/edit`：并行调用 getKnowledgeFile 加载文件元信息（文件名）与 getKnowledgeFileContent 加载内容，左右分栏（左 TextArea 编辑 Markdown，右 react-markdown + remark-gfm 实时预览，左右使用相同高度设置），底部右下角（flex-end justify）提供"保存"（调用 updateKnowledgeFileContent）与"关闭"按钮，无返回按钮
- API 服务封装：知识库/知识文件 CRUD + 状态切换 + 内容专用接口（getKnowledgeFileContent/updateKnowledgeFileContent，路径 /knowledge-bases/{kbId}/files/{id}/content），路径基于 /knowledge-bases 与 /knowledge-bases/{kbId}/files
- updateKnowledgeFileContent 请求体为原始字符串并覆盖 Content-Type: text/plain（与后端 consumes 对齐，避免 415）；getKnowledgeFileContent 返回 ApiResponse.data 内容字符串
- KnowledgeFile 类型不含 fileContent 字段，KFFormData 仅 fileName/fileDescription，文件内容通过内容专用接口单独读写