# Agent 调试平台

基于多智能体协同的 AI 应用调试与管理平台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus 3.5.7 + SQLite |
| 前端 | React 18 + TypeScript 5 + Vite 5 + Ant Design 5 |
| 构建 | Maven（后端）+ Vite（前端） |

## 功能模块

- **LLM 模型管理** — 模型配置的增删改查，支持 OpenAI / Anthropic / Azure / Ollama / DeepSeek / 自定义平台
- **工具注册与管理** — 工具元数据注册，支持 Java / TypeScript / Python 三种工具类型
- **HOOK 管理** — 生命周期钩子配置，覆盖 SESSION_START/END、BEFORE/AFTER MESSAGE、BEFORE/AFTER TOOL_CALL 六个阶段
- **智能体配置** — 智能体名称、系统提示词、关联模型与工具的配置管理
- **智能体执行引擎** — 多智能体会话执行，支持工具调用与推理链

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
