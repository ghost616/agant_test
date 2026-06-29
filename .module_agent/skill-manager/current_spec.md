SKILL 的 CRUD、工具绑定、SKILL 管理器
## SKILL CRUD 后端

已完成 SKILL CRUD 后端实现，包含：
- 实体层：SkillConfig（继承 BaseEntity）、SkillTool（关联表）
- Mapper 层：SkillConfigMapper、SkillToolMapper
- DTO 层：SkillConfigDTO、SkillCreateRequest、SkillUpdateRequest
- Service 层：SkillConfigService 接口 + SkillConfigServiceImpl 实现（事务支持、名称去重、工具 ID 校验）
- Controller 层：SkillConfigController（/api/skills）
- 错误码：SKILL_NOT_FOUND、SKILL_ALREADY_EXISTS
- 数据库表：skill_config、skill_tool（含索引）
- name 字段添加 `@Pattern(regexp = "^[a-z0-9_]+$")` 格式校验，与工具 name 验证保持一致
- SkillUpdateRequest 的 name 字段添加 `@Pattern` 注解
- SkillConfigController.update 方法添加 `@Valid` 注解，使 DTO 校验生效
- SkillConfigDTO 新增 `skillTools` 字段（`List<ToolConfigDTO>`），为非持久化字段，在加载 SKILL 时由 agent-engine 查询 skill_tool 关联表后填充，用于在 AgentExecutionContext 中传递完整的 SKILL 及其工具信息。
- SkillCreateRequest 和 SkillUpdateRequest 的 name 字段 @Pattern 正则更新为 `^(?!_sys_)[a-z0-9_]+$`，禁止以 `_sys_` 开头