智能体的 CRUD、关联默认 LLM 模型、挂载/卸载工具和 HOOK、行为参数配置
## 后端 CRUD 实现

已完成智能体配置模块的完整后端 CRUD 实现：

- **Entity**: AgentConfig（继承 BaseEntity，含 name/description/systemPrompt/modelId/status）、AgentTool（独立实体，含 agentId/toolId）
- **Mapper**: AgentConfigMapper、AgentToolMapper（均继承 BaseMapper）
- **DTO**: AgentConfigDTO（含 toolIds 列表）、AgentCreateRequest（name 必填）、AgentUpdateRequest
- **Service**: AgentConfigService 接口 + AgentConfigServiceImpl 实现
  - list(name, status): 列表查询，支持名称模糊和状态筛选
  - getById(id): 查询单个含关联 toolIds
  - create(request): 创建智能体 + 批量插入 agent_tool（事务）
  - update(id, request): 更新智能体 + 重建 agent_tool 关联（事务）
  - delete(id): 逻辑删除 + 清理 agent_tool（事务）
  - toggleStatus(id, status): 切换启用/禁用
- **Controller**: AgentConfigController，路径 /api/agents
  - GET / — 列表
  - GET /{id} — 详情
  - POST / — 创建
  - PUT /{id} — 更新
  - DELETE /{id} — 删除
  - PUT /{id}/status — 切换状态
- **ErrorCode**: 新增 AGENT_NOT_FOUND、AGENT_ALREADY_EXISTS
- **Entity**: AgentConfig 新增 recentMessageCount 字段（@TableField("recent_message_count")，默认值 10）
- **DTO**: AgentConfigDTO、AgentCreateRequest、AgentUpdateRequest 均新增 recentMessageCount 字段
- **Service**: create() 将 request.getRecentMessageCount() 写入 entity；update() 新增判断 if null 后更新；toDTO() 新增 .recentMessageCount() 映射