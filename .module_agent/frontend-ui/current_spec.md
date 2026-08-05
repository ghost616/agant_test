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
- 2026-08-05 新增 SILICONFLOW 平台：PlatformType 联合类型含 'SILICONFLOW'；PLATFORM_TYPE_LABELS 含 SILICONFLOW: '硅基流动'；SILICONFLOW 平台 modelName 恒为自由文本输入框（isSiliconFlow 使 modelNameSelectOptions 为空），其余平台保留 modelNames 下拉逻辑
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