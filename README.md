# Agent 调试平台

AI 智能体调试与管理平台，基于 opencode + DeepSeek 及 module_agent 插件开发，由 AI 完成需求设计、代码规范编写、模块设计、编码实现与代码审查，人工进行方案决策与把关。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus 3.5.7 + SQLite |
| 前端 | React 18 + TypeScript 5 + Vite 5 + Ant Design 5 |
| 构建 | Maven（后端）+ Vite（前端） |

## 功能模块

- **LLM 模型管理** — 模型配置的增删改查，支持 OpenAI / Anthropic / Azure / Ollama / DeepSeek / 自定义平台；各平台模型调用器均已实现，当前仅 DeepSeek 已完成连通性验证及全流程测试
- **工具注册与管理** — 工具元数据注册，支持 Java / TypeScript / Python / MCP HTTP 四种工具类型
- **SKILL 技能管理** — 技能配置的 CRUD，关联工具列表；智能体执行引擎按名称加载技能，自动注入提示词和关联工具
- **HOOK 管理** — HookInvoker 接口与 SystemHook 接口已定义，覆盖 SESSION_START/END、BEFORE/AFTER MESSAGE、BEFORE/AFTER TOOL_CALL 六个生命周期阶段，HOOK 管理功能后续实现
- **智能体配置** — 智能体名称、系统提示词、关联模型与工具的配置管理，支持最近消息数量控制与技能关联
- **智能体执行引擎** — 会话式智能体执行，支持工具调用与推理链，支持会话/对话变量、历史消息折叠、技能注入、推理内容透传
- **子会话执行** — 智能体在对话中可通过系统工具 `callback_sub_session` 创建子会话并异步执行指定的用户消息，支持为子会话分配工具与技能列表；子会话与父会话共享会话变量/对话变量（子会话的变量读写委托给父会话）；提供子会话列表查询和完成回调 API，前端可展示子会话执行情况

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
│       ├── controller/                  # REST 控制器
│       ├── service/                     # 业务服务实现
│       │   ├── agent/                   # 智能体服务实现
│       │   ├── model/                   # 模型配置服务
│       │   ├── session/                 # 会话服务
│       │   ├── tool/                    # 工具配置服务
│       │   └── skill/                   # 技能配置服务
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
    │   └── sessions/                    # 会话交互
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

7. **变量管理** — **会话变量**跨轮持久化至 `session_variable` 表，**对话变量**单轮有效、自动清除；工具执行期间通过 VariableProxy 代理对象提供统一的读写接口

8. **流式推送** — 整个对话过程通过 SSE 将增量内容、推理过程、工具调用指令、变量变更、完成状态实时推送到前端

## 四种工具实现方式

| 类型 | 实现方式 | 第三方开发指南 |
|------|----------|----------------|
| Java | 实现 ToolInvoker 接口，全限定类名注册，反射加载委托执行 | 项目中实现接口 → 编译放入 classpath → 注册填入类名 |
| TypeScript | index.ts 导出 execute(ctx, args) 函数；_runner.ts 桥接文件可手动放入或首次执行时自动生成；bun 优先，node+tsx fallback | 环境依赖 bun 或 node+tsx 任一可用 → 创建目录 → 编写 index.ts，函数签名为 execute(ctx: AgentExecutionContext, args: string): string，从 ./_runner 导入类型，args 为 JSON 字符串格式的工具参数，返回执行结果字符串 → 注册填入目录路径 |
| Python | index.py 定义 execute(context, arguments) 函数；_runner.py 桥接文件可手动放入或首次执行时自动生成；python3 优先，python fallback | 环境依赖 Python 3.10+ → 创建目录 → 编写 index.py，函数签名为 execute(context, arguments)，从 _runner 模块导入类型，arguments 为字典格式的工具参数，返回执行结果字符串 → 注册填入目录路径 |
| MCP HTTP | 注册服务 URL，运行时通过 JSON-RPC 协议发现远程工具并自动展开；支持 Bearer Token 认证 | 部署 MCP 协议服务 → 注册填入 URL 和 Token |
