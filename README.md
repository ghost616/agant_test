# Agent 调试平台

AI 智能体调试与管理平台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus 3.5.7 + SQLite |
| 前端 | React 18 + TypeScript 5 + Vite 5 + Ant Design 5 |
| 构建 | Maven（后端）+ Vite（前端） |

## 功能模块

- **LLM 模型管理** — 模型配置的增删改查，支持 OpenAI / Anthropic / Azure / Ollama / DeepSeek / 自定义平台
- **工具注册与管理** — 工具元数据注册，支持 Java / TypeScript / Python / MCP HTTP 四种工具类型
- **HOOK 管理** — HookInvoker 接口与 SystemHook 接口已定义，覆盖 SESSION_START/END、BEFORE/AFTER MESSAGE、BEFORE/AFTER TOOL_CALL 六个生命周期阶段，HOOK 管理功能后续实现
- **智能体配置** — 智能体名称、系统提示词、关联模型与工具的配置管理
- **智能体执行引擎** — 会话式智能体执行，支持工具调用与推理链

## 快速启动

### 一键启动（Windows）

```bash
dev.bat
```

脚本会自动完成：安装前端依赖 → 启动后端（端口 8080）→ 等待后端就绪 → 启动前端（端口 3000）。
前端退出时自动关闭后端进程。

### 分别启动

```bash
# 1. 启动后端
mvn spring-boot:run

# 2. 启动前端
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
├── pom.xml                              # Maven 构建配置
├── package.json                         # 前端依赖配置
├── vite.config.ts                       # Vite 构建配置（/api 代理）
├── tsconfig.json                        # TypeScript 配置
├── dev.bat                              # 开发模式一键启动
├── build.bat                            # 一键编译打包
│
├── src/main/java/com/ghost616/platform/
│   ├── PlatformApplication.java         # Spring Boot 启动类
│   ├── config/                          # 配置（CORS、MyBatis-Plus、全局异常处理）
│   ├── controller/                      # REST 控制器
│   ├── service/                         # 业务服务层
│   ├── repository/                      # 数据访问层（Mapper）
│   ├── entity/                          # 数据库实体
│   ├── dto/                             # 数据传输对象
│   ├── enums/                           # 枚举（ErrorCode、PlatformType、ToolType 等）
│   ├── exception/                       # 异常定义（BaseException、BusinessException）
│   ├── event/                           # Spring 事件（ToolChangedEvent）
│   └── util/                            # 工具类
│
├── src/main/resources/
│   ├── application.yml                  # 应用配置（SQLite、MyBatis-Plus）
│   └── schema.sql                       # DDL 初始化脚本
│
└── src/                                 # 前端源码
    ├── main.tsx                         # React 入口
    ├── App.tsx                          # 路由与 antd 布局
    ├── index.css                        # 全局样式
    ├── pages/                           # 页面组件
    │   ├── models/                      # 模型管理
    │   ├── tools/                       # 工具管理
    │   ├── agents/                      # 智能体配置
    │   └── sessions/                    # 会话交互
    ├── components/                      # 通用组件
    ├── services/                        # API 请求封装
    ├── hooks/                           # 自定义 Hooks
    ├── types/                           # TypeScript 类型定义
    └── utils/                           # 工具函数
```

## 智能体执行流程

1. **上下文加载** — 根据会话 ID 加载智能体配置（系统提示词、默认模型、已挂载的工具列表），同时从数据库恢复历史消息记录，构建执行上下文并缓存

2. **消息组装** — 将用户消息保存入库，拼装完整的消息列表（系统提示词 + 历史对话 + 工具调用记录），并将工具定义统一格式化后传给模型

3. **HOOK 触发** — 在会话启动时、每条消息发送前、消息接收完成后，自动扫描并执行已注册的 HOOK 处理器，系统级 HOOK 在每个阶段后额外按优先级执行

4. **模型调用** — 根据智能体配置的平台类型匹配对应的调用适配器，以流式方式请求 LLM，实时解析 SSE 或 NDJSON 格式的增量回复

5. **工具调度** — 若模型回复中包含工具调用指令，前端异步提交执行任务，后端从会话工具列表中查找对应工具实例执行，执行完成后将结果写回消息历史和上下文，继续下一轮模型调用

6. **流式推送** — 整个对话过程通过 SSE 将增量内容、推理过程、工具调用指令、完成状态实时推送到前端
