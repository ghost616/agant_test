# Agent 调试平台

AI 智能体调试与管理平台，基于 opencode + DeepSeek 及 module_agent 插件开发，由 AI 完成需求设计、代码规范编写、模块设计、编码实现与代码审查，人工进行方案决策与把关。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus 3.5.7 + SQLite |
| 前端 | React 18 + TypeScript 5 + Vite 5 + Ant Design 5 |
| 构建 | Maven（后端）+ Vite（前端） |

## 功能模块

- **LLM 模型管理** — 模型配置的增删改查，支持 OpenAI / Anthropic / Azure / Ollama / DeepSeek / 自定义平台，各平台均已接入，当前仅 DeepSeek 完成连通性验证及全流程测试
- **工具注册与管理** — 工具元数据注册，支持 Java / TypeScript / Python / MCP HTTP / CUSTOM 五种工具类型，CUSTOM 为扩展工具类型，可通过扩展接入特殊工具能力
- **SKILL 技能管理** — 技能配置的增删改查，关联工具列表；智能体执行引擎按名称加载技能，自动注入提示词和关联工具
- **HOOK 管理** — 覆盖智能体六个生命周期阶段的钩子扩展机制，实现后放入工程即可被自动发现加载；内置会话消息保存钩子自动运行，无需管理界面
- **智能体配置** — 智能体名称、系统提示词、关联模型与工具的配置管理，支持最近消息数量控制与技能关联
- **智能体执行引擎** — 会话式智能体执行，支持工具调用与推理链，支持会话/对话变量、历史消息折叠、技能注入、推理内容透传；内置跨会话消息通信能力（历史同步、变量广播、子会话事件等）
- **子会话执行** — 智能体在对话中可通过系统工具创建子会话并异步执行指定的用户消息，支持为子会话分配工具与技能列表；子会话与父会话共享会话变量/对话变量；提供子会话列表查询和完成回调能力，前端可展示子会话执行情况
- **三域会话授权** — 工具和技能支持三种会话作用域：所有会话可用、只能父会话使用、只能子会话使用
- **智能体评估管理** — 评估模板的创建与管理，评估项配置（关联模型、执行次数、执行模式），评估任务执行，评估结果查看与结论分析
- **知识库管理** — 知识库 CRUD、文件上传与 Markdown 编辑、ES 索引自动生成、发布状态管理、智能体-知识库绑定。发布流程：创建知识库 → 上传或编辑文件 → 发布到 ES
- **知识库搜索** — 智能体通过内置工具搜索知识库内容。搜索流程：智能体绑定知识库后自动注入搜索工具 → 支持向量/全文/混合三种检索方式 → 返回搜索结果
- **对话ID** — 用户每次发送消息生成唯一对话ID，串联同一轮对话的父子会话所有消息；作用包括会话历史查看完整对话链、对话详情按时间序展示、工具调用配对，并为归档分析提供基础
- **会话历史** — 页面展示主会话列表，点击进入用户消息列表，有对话ID的消息可进入对话详情页；详情页表格区分父/子会话，支持 Markdown 渲染、工具调用/结果关联展示、弹窗对话流浏览
- **智能体日志** — 覆盖智能体运行全流程关键环节（模型调用、工具执行、消息处理、上下文构建、会话与错误等场景）的日志记录，每条日志含日志类型与日志等级，贯穿核心执行链路实现全流程可追踪
- **运行日志** — 智能体日志的持久化存储与查询：日志列表支持按会话名模糊搜索、日志类型与日志等级筛选、分页展示，日志数据及会话/对话变量快照超长内容支持展开详情查看；自动清理 30 天前的历史日志，控制存储占用

## 快速启动

### 一键启动（Windows）

```bash
dev.bat
```

脚本会自动完成：安装前端依赖 → 启动后端（端口 8080）→ 等待后端就绪 → 启动前端（端口 3000）。
前端退出时自动关闭后端进程。

### 分别启动

```bash
# 1. 编译安装依赖模块
mvn install -DskipTests

# 2. 启动后端
mvn spring-boot:run -f platform-app/pom.xml

# 3. 启动前端
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:3000`，API 请求自动代理到后端 `http://localhost:8080`。

### 一键打包

```bash
build.bat
```

编译前端并打包为 Spring Boot 可执行 JAR。

## 项目结构

```
├── pom.xml                              # 父 POM（packaging=pom，聚合 3 个子模块）
├── package.json                         # 前端依赖配置
├── vite.config.ts                       # Vite 构建配置（/api 代理）
├── tsconfig.json                        # TypeScript 配置
├── dev.bat                              # 开发模式一键启动
├── build.bat                            # 一键编译打包
│
├── agent-base/                          # 核心抽象层（Maven 子模块）
│   ├── pom.xml
│   └── src/main/java/com/ghost616/agentbase/
│       ├── service/agent/               # 上下文管理、会话管理、消息代理
│       │   └── invoker/                 # 工具调用器 & HOOK 契约
│       ├── service/model/invoker/       # 模型调用器抽象接口
│       ├── dto/                         # 传输对象（model/skill/tool/chat）
│       ├── enums/                       # 枚举（ErrorCode、HookPhase、ToolType 等）
│       ├── exception/                   # 异常定义（BaseException、BusinessException）
│       ├── event/                       # 事件定义（ToolChangedEvent）
│       └── util/                        # 工具类（JsonMapper）
│
├── agent-integration/                   # 模型集成实现层（Maven 子模块）
│   ├── pom.xml
│   └── src/main/java/com/ghost616/agentinteg/
│       ├── model/invoker/               # 6 大平台 ModelInvoker 实现
│       ├── AgentAssembler.java          # Build 组装类
│       └── tool/                        # 集成层系统工具
│
├── platform-app/                        # 应用层（Maven 子模块，可执行 JAR）
│   ├── pom.xml
│   └── src/main/java/com/ghost616/platform/
│       ├── PlatformApplication.java     # Spring Boot 启动类
│       ├── config/                      # 配置（CORS、MyBatis-Plus、异常处理等）
│       ├── controller/                  # REST 控制器（含 KnowledgeBaseController、KnowledgeFileController）
│       ├── service/                     # 业务服务实现
│       │   ├── agent/                   # 智能体服务实现
│       │   ├── model/                   # 模型配置服务
│       │   ├── session/                 # 会话服务
│       │   ├── tool/                    # 工具配置服务
│       │   ├── skill/                   # 技能配置服务
│       │   └── knowledge/               # 知识库服务实现
│       ├── repository/                  # 数据访问层（Mapper）
│       ├── entity/                      # 数据库实体
│       ├── dto/                         # 应用层 DTO
│       ├── enums/                       # 平台枚举（PlatformType）
│       └── systemtest/                  # 系统测试
│   ├── src/main/resources/
│   │   ├── application.yml              # 应用配置（SQLite、MyBatis-Plus）
│   │   └── schema.sql                   # DDL 初始化脚本
│
└── src/                                 # 前端源码
    ├── main.tsx                         # React 入口
    ├── App.tsx                          # 路由与 antd 布局
    ├── index.css                        # 全局样式
    ├── pages/                           # 页面组件
    │   ├── models/                      # 模型管理
    │   ├── tools/                       # 工具管理
    │   ├── agents/                      # 智能体配置
    │   ├── skills/                      # 技能管理
    │   ├── sessions/                    # 会话交互
    │   ├── evaluations/                 # 智能体评估
    │   └── knowledge/                   # 知识库管理（KnowledgeBaseList、KnowledgeFileList、KnowledgeFileEdit）
    ├── components/                      # 通用组件
    ├── services/                        # API 请求封装
    ├── hooks/                           # 自定义 Hooks
    ├── types/                           # TypeScript 类型定义
    └── utils/                           # 工具函数
```

## 智能体执行流程

1. **上下文加载** — 根据会话 ID 加载智能体配置（系统提示词、默认模型、已挂载的工具列表），同时从数据库恢复历史消息记录与**会话变量**，加载关联的**技能列表**，构建执行上下文并缓存

2. **消息组装** — 将用户消息保存入库，按**消息分组**机制拼装消息列表：以 user 消息为分界点进行分组，若消息组总数超出 `recentMessageCount` 限制则折叠早期消息组（仅保留 user 消息并插入一条占位 assistant 消息）；展开机制通过 `_sys_history_query` 系统工具设置 `_sys_his_msgs_index` 对话变量标记展开索引，下轮折叠时跳过该组。最终拼装为：系统提示词 + 历史消息 + 工具调用记录 + 推理内容

3. **技能注入** — 系统提示词后追加当前会话已加载的可用技能列表说明，将每个已加载技能的提示词注入消息上下文，关联工具按名称去重合并到工具定义列表中；通过 `_sys_load_skills`（LoadSkillsSystemTool）和 `_sys_unload_skills`（UnloadSkillsSystemTool）系统工具可在对话中动态加载/卸载技能

4. **HOOK 触发** — 在会话启动时、每条消息发送前、消息接收完成后，自动扫描并执行已注册的 HOOK 处理器，系统级 HOOK 在每个阶段后额外按优先级执行

5. **模型调用** — 根据智能体配置的平台类型匹配 agent-integration 模块中对应的 ModelInvoker 实现（OpenAI/Ollama/Anthropic/Azure/DeepSeek/Custom），以流式方式请求 LLM，实时解析流式回复（含 **reasoning 推理内容**）

6. **工具调度** — 若模型回复中包含工具调用指令（可能在推理/思考过程中决定调用工具），后端先将工具调用数据缓存至队列，前端再逐条拉取异步提交执行任务，后端从会话工具列表中查找对应工具实例，通过 agent-base 模块中的工具调用器（JavaToolInvoker/TypeScriptToolInvoker/PythonToolInvoker/McpHttpToolInvoker）执行，执行完成后将结果写回消息历史和上下文，继续下一轮模型调用

7. **变量管理** — **会话变量**跨轮持久化至 `session_variable` 表，**对话变量**单轮有效、自动清除；工具执行期间通过 VariableProxy 代理对象提供统一的读写接口。变量变更通过 sendmessage 实时 SSE 推送至前端（PUT/REMOVE 语义）

8. **流式推送** — 整个对话过程通过 SSE 将增量内容、推理过程、工具调用指令、变量变更、完成状态实时推送到前端

## Responses API

### 概念说明

Responses API 是新一代模型请求接口（`/v1/responses` 端点），请求结构从 Chat Completions 的 messages 列表调整为 `instructions` + `input`：系统提示词与技能说明作为 `instructions` 独立传递，对话消息放入 `input`，且服务端可维护会话状态，响应携带 `responseId` 供多轮续接。

与 Chat Completions（`/v1/chat/completions`，每次请求需携带完整 messages 历史）相比，Responses API 支持两种模式：

- **有状态（responses）** — 服务端保存对话状态，多轮对话通过 `previousResponseId` 引用上一轮响应，无需重复发送历史消息，`input` 仅需从最后一条 user 消息开始，可显著减少请求体体积与 token 消耗
- **无状态（responses_stateless）** — 不依赖服务端会话状态，每次请求发送完整消息历史（`input` 为全量消息），模型独立处理每轮请求，适合无需跨轮续接的场景

### 使用方式

在模型配置中选择请求类型（RequestType 枚举：`RESPONSES` 有状态 / `RESPONSES_STATELESS` 无状态 / `COMPLETIONS` 传统 Chat Completions），智能体执行引擎依据该配置自动匹配对应的请求路径与模型调用器。

有状态模式的多轮续接机制：引擎将会话最近一次模型响应携带的 `responseId` 记录为会话 `lastResponseId`，下一轮请求自动透传为 `previousResponseId`（会话级 `lastResponseId` 优先于 API 请求传入值）；流式过程中从 `response.completed` 事件捕获 `responseId` 写回会话上下文，从而实现无需重发历史的多轮续接。无状态模式不传 `previousResponseId`，每轮携带全量历史消息。

### 支持平台

已实现 6 个平台的 Responses API 模型调用器，均位于 agent-integration 模块 `model/invoker` 包下：

| 平台 | 调用器 | 说明 |
|------|--------|------|
| OpenAI | OpenAIResponsesInvoker | 基础实现，`/v1/responses` 端点，Bearer 认证 |
| DeepSeek | DeepSeekResponsesInvoker | 复用 OpenAI Responses 兼容实现 |
| Kimi（月之暗面） | KimiResponsesInvoker | OpenAI 兼容实现，按模型微调 reasoning 参数 |
| 火山引擎 | VolcEngineResponsesInvoker | 复用 OpenAI Responses 兼容实现 |
| Azure | AzureResponsesInvoker | `/openai/deployments/{model}/responses?api-version=...` 端点，api-key 认证 |
| 自定义 | CustomResponsesInvoker | 通用 OpenAI Responses 兼容端点 |

## 智能体评估流程

1. **评估模板创建** — 选择被评估的智能体，配置评估模板的名称与描述，完成模板基本信息定义

2. **评估项配置** — 关联已创建的评估模板，选择评估用的模型、设置单轮执行次数、选择执行模式（BACKGROUND 后台静默执行 / FOREGROUND 前台流式执行），可配置多个评估项

3. **基准会话设定** — 创建评估项时自动生成基准会话，复制被评估智能体的系统提示词、工具和技能配置到该会话；用户在基准会话中输入一条或多条标准用户消息作为评估输入，评估项将基于这些消息对模型进行评测

4. **执行机制** — 每次执行时复制基准会话创建独立执行会话，继承基准会话的全部配置和用户消息，按序发送所有用户消息。BACKGROUND 模式下后台异步逐次执行所有评估项，执行完成后汇总结果；FOREGROUND 模式下前台通过 SSE 流式实时推送执行日志，包含模型调用的思考过程和推理内容

5. **结果查看** — 评估详情页展示每轮对话的完整消息对比（请求/响应），以及基于评估标准生成的 Markdown 格式评估结论，支持逐项排查执行质量

## 四种工具实现方式

| 类型 | 实现方式 | 第三方开发指南 |
|------|----------|----------------|
| Java | 实现 ToolInvoker 接口，全限定类名注册，反射加载委托执行 | 项目中实现接口 → 编译放入 classpath → 注册填入类名 |
| TypeScript | index.ts 导出 execute(ctx, args) 函数；_runner.ts 桥接文件可手动放入或首次执行时自动生成；bun 优先，node+tsx fallback | 环境依赖 bun 或 node+tsx 任一可用 → 创建目录 → 编写 index.ts，函数签名为 execute(ctx: AgentExecutionContext, args: string): string，从 ./_runner 导入类型，args 为 JSON 字符串格式的工具参数，返回执行结果字符串 → 注册填入目录路径 |
| Python | index.py 定义 execute(context, arguments) 函数；_runner.py 桥接文件可手动放入或首次执行时自动生成；python3 优先，python fallback | 环境依赖 Python 3.10+ → 创建目录 → 编写 index.py，函数签名为 execute(context, arguments)，从 _runner 模块导入类型，arguments 为字典格式的工具参数，返回执行结果字符串 → 注册填入目录路径 |
| MCP HTTP | 注册服务 URL，运行时通过 JSON-RPC 协议发现远程工具并自动展开；支持 Bearer Token 认证 | 部署 MCP 协议服务 → 注册填入 URL 和 Token |
| CUSTOM | 扩展工具类型，支持通过子工具类型（当前支持 BROWSER）实现特殊工具能力 | 实现子工具类型对应的调用器并注册 → 注册工具时选择 CUSTOM 类型并指定子工具类型 |

## 浏览器工具

BROWSER 浏览器工具通过客户端执行机制，在用户浏览器环境中执行工具操作。

**执行原理：**

服务端 BrowserToolInvoker 发起调用 → 委托回调 BrowserToolCallbackImpl 阻塞等待 → 前端通过 ToolHostBridge JS 桥接获取执行上下文 → 调用 browser_tool_executor.js 引擎中的工具函数 → 结果回调至服务端，唤醒阻塞等待完成。

## 知识库配置

### 配置流程

1. **创建知识库** — 在知识库管理页面创建知识库，填写名称与描述，完成基本信息定义
2. **ES 索引自动生成** — 创建知识库时自动生成 ES 索引名称，前端仅列表页展示，编辑弹窗不可修改，用于后续文件内容的向量化存储与检索
3. **上传 / 编辑文件** — 向知识库上传 Markdown 格式文件，或在页面内直接编辑文件内容
4. **发布到 ES** — 文件发布时文本按 **5000 字符**分批向量化写入 ES Index，完成内容检索的就绪
5. **绑定智能体** — 将知识库绑定到目标智能体，绑定后该智能体的会话自动获得知识库检索能力

### 发布状态

知识库文件发布状态通过枚举管理，共五种状态：

- **UNPUBLISHED** — 未发布
- **PUBLISHING** — 发布中
- **PUBLISHED** — 已发布
- **PENDING_PUBLISH** — 待发布
- **PUBLISH_ERROR** — 发布失败

### 发布机制

文件发布时，服务端将文件文本内容按 5000 字符分批切分，逐批向量化后写入 ES Index；任一状态流转（发布中、成功、失败）均会实时更新到文件记录，供前端展示与后续重试。

## 知识库工具

智能体绑定知识库后，以下 4 个内置工具（`default_tool_rag_*` 前缀）自动注入会话工具列表，会话内直接可用。

| 工具名称 | 用途描述 | 关键参数 |
|----------|----------|----------|
| default_tool_rag_info | 获取当前会话关联的知识库信息 | 无参数 |
| default_tool_rag_file_info | 搜索知识库中的文件 | knowledgeBaseId、fileName、searchLimit |
| default_tool_rag_search | 搜索知识库文本块 | knowledgeBaseId、fileId、searchType、query、searchLimit、contextLines |
| default_tool_rag_file_chunk | 获取指定文件的文本块 | knowledgeBaseId、fileId、startLine、endLine |

**自动绑定说明**：智能体绑定知识库后，4 个工具自动注入会话工具列表，无需手动配置即可在会话中调用。
