工具的注册（Java/TS/Python 三种类型）、元信息管理、执行调度、工具管理器
## 基础模型层

- REST API: GET/POST/PUT/DELETE /api/tools，带筛选/名称去重（list 接口已去掉分页，直接返回 List）
- ToolCreateRequest.implPath 增加 @NotBlank(message = "实现路径不能为空") 校验注解，确保创建工具时必须提供实现路径。
- GET /api/tools/{name}/impl：按工具名称查询工具实现信息，返回 ToolConfigDTO。Controller 新增 getImplByName 端点；Service 接口新增 getImplByName(String name) 方法，ServiceImpl 通过 LambdaQueryWrapper 按名称等值查询，不存在时抛出 TOOL_NOT_FOUND 异常。
- ToolConfigDTO.id 字段添加 @JsonSerialize(using = ToStringSerializer.class) 注解，防止前端 JavaScript 处理雪花 ID 时精度丢失。
## Schema 验证与规范化

- ToolConfigServiceImpl.create() 和 update() 在设置 parameterSchema 前调用 normalizeParameterSchema() 进行 JSON 合法性校验
- normalizeParameterSchema()：null/空字符串返回 null；无效 JSON 抛 BusinessException(TOOL_SCHEMA_INVALID)；校验通过后原始 JSON 字符串原样返回，不做任何格式转换、包装或 type 字段校验
- type 包装由 ModelInvoker.toToolDefinition() 负责处理
- validateImplPath()：create/update 调用，根据工具类型校验实现路径有效性
  -- JAVA：Class.forName() 验证类存在，ClassNotFoundException 抛 TOOL_SCHEMA_INVALID
  -- TYPESCRIPT：Files.isDirectory() 验证目录存在 + index.ts 存在，否则抛 TOOL_SCHEMA_INVALID
  -- PYTHON：Files.isDirectory() 验证目录存在 + index.py 存在，否则抛 TOOL_SCHEMA_INVALID
## Python 工具执行器

- PythonToolInvoker 实现 ToolInvoker 接口，支持 Python 3.10+ 工具的进程执行
- 运行时检测：优先 python3，其次 python，Windows 通过 cmd /c 包装调用，类 Unix 直接调用
- 从 classpath /agent/_runner.py 加载桥接模板，运行时生成到工具目录
- 序列化上下文为 _input.json → 执行 python _runner.py _input.json → 30 秒超时 → 返回输出
- ToolManager.getInvoker() 中 case PYTHON 路由到 PythonToolInvoker
- _runner.py 桥接脚本：解析 JSON → 动态导入 index.py → 调用 execute(context, arguments) → 输出结果
_runner.py AgentExecutionContext 新增 recent_message_count 属性，从 data 中解析 recentMessageCount 字段
## 代码审查修复

- ContextSerializer：提取 serializeToJson() 为静态工具类，消除 TypeScriptToolInvoker 和 PythonToolInvoker 中的重复代码
- RUNNER_TEMPLATE 改为静态内部类（RunnerTemplate.INSTANCE）懒加载，避免 ExceptionInInitializerError
- execute() 中日志从 log.info 降级为 log.debug，避免敏感参数泄露
## MCP HTTP 远程工具调用器

- 新增 ToolType.MCP_HTTP 枚举值
- 实体/DTO/请求新增 authConfig 字段，create/update/toDTO 方法处理该字段
- validateImplPath() 新增 MCP_HTTP 校验（URL 须 http:// 或 https:// 开头）
- McpHttpToolInvoker 实现 ToolInvoker，基于 mcp-java-sdk 2.0.0：
  -- 构造函数解析 authConfig JSON（bearer token 或 apikey），通过 HttpClientSseClientTransport 的 requestBuilder 设置 HTTP 头
  -- execute() 创建 McpSyncClient → initialize() → callTool() → 提取 TextContent 文本 → close()
- ToolManager.getInvoker() 新增 case MCP_HTTP 路由
validateImplPath() MCP_HTTP 分支在 URL 格式校验通过后，新增 HTTP HEAD 连通性预测试（connect 3s / read 3s 超时），连接失败或非 2xx 响应抛出 TOOL_SCHEMA_INVALID 异常，确保注册/编辑时即可发现不可达端点。
validateImplPath() MCP_HTTP 分支：URL 格式校验通过后，通过 McpSyncClient.initialize() 验证 MCP 协议握手（5s 超时），成功调用 closeGracefully() 关闭；异常抛出 TOOL_SCHEMA_INVALID "MCP 服务连接失败: {url}"。替换了原先的 HTTP HEAD 可达性测试。
validateImplPath() MCP_HTTP 分支连通性测试使用 HttpClientSseClientTransport（SSE 传输）替代 HttpClientStreamableHttpTransport，通过 SSE 端点验证 MCP 协议握手。
validateImplPath() 增加 authConfig 参数，MCP_HTTP 分支连通性测试通过 McpAuthConfigParser.parse() 解析认证 headers，构建 HttpRequest.Builder 传入 HttpClientSseClientTransport.Builder.requestBuilder()，确保带认证的 MCP 端点也能通过握手验证。
validateImplPath() MCP_HTTP 分支：authConfig 增加 null/blank 判空，解析异常时 log.warn 并降级为无认证连接，参考 SessionManager.expandMcpTools() 的 try-catch 模式。