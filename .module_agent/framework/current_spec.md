平台根配置文件、公共基础设施代码、共享 DTO/枚举/异常/工具类
## 功能概述

系统框架模块负责管理项目根配置文件、公共基础设施代码。包含 Maven 构建配置（pom.xml）、前端构建配置（package.json, vite.config.ts, tsconfig.json）、应用启动类、统一响应体（ApiResponse/PageResult）、实体基类（BaseEntity）、枚举定义（ErrorCode/CommonStatus/ToolType/HookPhase/PlatformType）、异常类（BaseException/BusinessException）、全局异常处理、CORS 配置，以及前端入口、路由布局、API 拦截器、类型定义和基础样式。
数据持久层已从 JPA+H2 升级为 MyBatis-Plus 3.5.7 + SQLite，使用 MybatisPlusConfig 配置分页插件和 MapperScan，MyMetaObjectHandler 自动填充 createTime/updateTime 字段。
- ErrorCode 新增模型管理错误码：MODEL_NOT_FOUND（模型配置不存在）、MODEL_ALREADY_EXISTS（模型名称已存在），编码为 MODEL-CONFIG-001/002。
- PlatformType 枚举和 TypeScript 平台类型已添加 DeepSeek 平台支持（DEEPSEEK）。
- PlatformType 枚举新增 defaultBaseUrl 和 defaultModelNames 字段，为每个平台提供默认接口地址和可用模型列表（AZURE/CUSTOM 需用户自行填写）。
- 前端路由新增 `/models/:id/test` 路径，指向 ModelTest 组件，用于模型功能测试页面。
- 前端入口已移除 `<React.StrictMode>` 外层包裹，避免开发模式下 useEffect 副作用重复执行。
[2026-06-11] pom.xml 的 `<description>` 和 PlatformApplication.java 的 Javadoc 中 "Agent 平台" 已更新为 "Agent 调试平台"。
- DeepSeek 平台默认模型名已更新：deepseek-chat → deepseek-v4-flash，deepseek-reasoner → deepseek-v4-pro
- schema.sql 新增 agent_config 表（智能体配置：name/description/system_prompt/model_id/status/create_time/update_time/deleted）和 agent_tool 中间表（agent_id/tool_id 关联），含相关索引。
HookPhase 枚举新增 SYSTEM_POST("系统后置执行") 阶段值，用于标识所有 HOOK 执行完毕后触发的系统级后置 HOOK。
HookPhase 枚举已恢复为原始六值：SESSION_START/END, BEFORE/AFTER MESSAGE, BEFORE/AFTER TOOL_CALL。
- ErrorCode 新增 TOOL_SCHEMA_INVALID 错误码（TOOL-SCHEMA-001，工具参数 Schema 格式无效），位于 TOOL_EXECUTE_ERROR 之后
dev.bat 在启动时通过 chcp 65001 切换控制台编码页为 UTF-8，确保脚本中中文字符正常显示。
dev.bat 生成的后端临时批处理同样先执行 chcp 65001 切换 UTF-8 编码，确保后端窗口中文输出正常。
tool_config 表新增 auth_config TEXT 列，用于存储工具认证配置；同时添加 ALTER TABLE 迁移语句以兼容已有数据库。
application.yml 中 spring.sql.init 新增 continue-on-error: true，使 SQL 初始化 ALTER TABLE 语句在列已存在时不阻断启动。
- pom.xml 的 MCP SDK 依赖从 umbrella `mcp` 替换为 `mcp-core` + `mcp-json-jackson2`（均 2.0.0），避免 Jackson 3.x（`mcp-json-jackson3`）与 Spring Boot 3.2.5 的 Jackson 2.x 冲突。同时 properties 新增 `<jackson.version>2.20.1</jackson.version>` 统一 Jackson 版本。
[2026-06-21] pom.xml 已移除 MCP SDK 依赖（mcp-core + mcp-json-jackson2 2.0.0），MCP 工具调用功能将在 tool-registry 模块中通过不同方式重构。
[2026-06-21] 新增 ToolChangedEvent 事件类（ApplicationEvent 子类），携带 toolId，供 Spring 事件机制在工具变更时发布/监听。
- schema.sql 新增 session_variable 表（会话变量：id 雪花主键/session_id/variable_key VARCHAR(255)/variable_value TEXT/create_time DATETIME），含 idx_session_variable_session_id 单列索引和 idx_session_variable_key(session_id, variable_key) 组合索引。
session_variable 表记录变更：移除 DATETIME 使用 TIMESTAMP 类型，补充缺失列 update_time 和 deleted，并新增 ALTER TABLE 迁移语句兼容已有数据库。
agent_config 表新增 recent_message_count INTEGER DEFAULT 10 列及对应 ALTER TABLE 迁移语句，用于控制会话中保留的最近消息数量。
新增 SchemaMigration 启动组件（@Component + ApplicationRunner + @Order(1)），通过 PRAGMA table_info 检测列是否存在并按需执行 ALTER TABLE，替代 schema.sql 中硬编码的 ALTER TABLE 迁移语句。迁移清单：tool_config.auth_config、session_variable.update_time、session_variable.deleted、agent_config.recent_message_count。application.yml 已移除无用的 spring.sql.init.continue-on-error 配置。
schema.sql 新增 agent_skill 中间表（agent_id/skill_id 关联），结构与 agent_tool 一致，含 agent_skill_agent_id 和 agent_skill_skill_id 两个索引。
## 文件结构

```
pom.xml                                          -- Maven 构建文件（Spring Boot 3, JDK 17）
package.json                                     -- 前端 npm 配置
vite.config.ts                                   -- Vite 构建配置（React 插件, /api 代理）
tsconfig.json                                    -- TypeScript 配置
src/main/java/com/ghost616/platform/
├── PlatformApplication.java                     -- Spring Boot 启动类
├── config/
│   ├── GlobalExceptionHandler.java               -- 全局异常处理
│   └── WebConfig.java                           -- CORS 配置
├── dto/
│   ├── ApiResponse.java                         -- 统一 API 响应体
│   └── PageResult.java                          -- 分页结果
├── entity/
│   └── BaseEntity.java                          -- 实体基类（@MappedSuperclass）
├── enums/
│   ├── ErrorCode.java                           -- 错误码枚举
│   ├── CommonStatus.java                        -- 启用/禁用状态
│   ├── ToolType.java                            -- 工具类型
│   ├── HookPhase.java                           -- HOOK 阶段
│   └── PlatformType.java                        -- LLM 平台类型
└── exception/
    ├── BaseException.java                       -- 基础异常
    └── BusinessException.java                   -- 业务异常
src/main/resources/
└── application.yml                               -- 应用配置（端口 8080, H2）
src/main.tsx                                      -- React 入口
src/App.tsx                                       -- 路由与布局
src/services/api.ts                              -- Axios 实例与拦截器
src/types/common.ts                              -- 公共 TypeScript 类型
src/index.css                                    -- 全局样式
```

build.bat                                         -- Windows 一键编译打包脚本
dev.bat                                           -- Windows 开发模式一键启动脚本
- src/main/resources/schema.sql -- DDL 初始化脚本，model_config 表定义
- README.md -- 项目说明文档，含技术栈、功能模块、快速启动指南、项目结构