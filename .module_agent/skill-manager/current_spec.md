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
