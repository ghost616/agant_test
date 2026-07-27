# platform-data 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## 数据实体层

提供 16 个数据实体类，均位于 `com.ghost616.platform.entity` 包下：
- **BaseEntity**：实体基类，含雪花ID、自动填充的 createTime/updateTime、逻辑删除标志
- **Message**：消息实体，映射 message 表，不继承 BaseEntity
- **AgentConfig**：智能体配置，继承 BaseEntity，映射 agent_config 表
- **AgentTool**：智能体-工具关联实体，映射 agent_tool 表
- **AgentSkill**：智能体-技能关联实体，映射 agent_skill 表
- **ModelConfig**：模型配置实体，继承 BaseEntity，映射 model_config 表
- **ToolConfig**：工具配置实体，继承 BaseEntity，映射 tool_config 表
- **SkillConfig**：SKILL 配置实体，继承 BaseEntity，映射 skill_config 表
- **SkillTool**：SKILL-工具关联实体，映射 skill_tool 表
- **Session**：会话实体，继承 BaseEntity，映射 session 表，含 isEvaluation 评估标记字段
- **MessageToolCall**：工具调用记录实体，映射 message_tool_call 表
- **SessionTool**：会话工具关联实体，映射 session_tool 表
- **SessionVariable**：会话变量实体，映射 session_variable 表
- **SessionSkill**：会话技能关联实体
- **Evaluation**：评估配置实体，继承 BaseEntity，映射 evaluation 表，含 name/description/benchmarkSessionId/executionCount
- **EvaluationResult**：评估结果实体，继承 BaseEntity，映射 evaluation_result 表，含 evaluationId/evaluationSessionId/result

枚举位于 `com.ghost616.platform.enums` 包：
- **SubToolType**：子工具类型枚举（BROWSER），@EnumValue 标记 code 字段
- **Evaluation** 字段更新：新增 modelId（Long，映射 model_id 列）
## 数据访问层

提供 15 个 MyBatis-Plus Mapper 接口，均位于 `com.ghost616.platform.repository` 包下：
- **MessageMapper**：继承 BaseMapper\<Message\>，额外提供 rollbackBySessionIdAndGeSequenceNum 批量更新方法
- **MessageToolCallMapper**：继承 BaseMapper\<MessageToolCall\>，额外提供 deleteByMessageIds 批量删除方法
- **SessionMapper**：继承 BaseMapper\<Session\>，额外提供 addTotalTokenUsed 原子增减方法
- **AgentConfigMapper、AgentToolMapper、AgentSkillMapper**：基础 CRUD Mapper
- **ModelConfigMapper、ToolConfigMapper**：基础 CRUD Mapper
- **SkillConfigMapper、SkillToolMapper**：基础 CRUD Mapper
- **SessionToolMapper、SessionVariableMapper、SessionSkillMapper**：基础 CRUD Mapper
- **EvaluationMapper**：继承 BaseMapper\<Evaluation\>，基础 CRUD Mapper
- **EvaluationResultMapper**：继承 BaseMapper\<EvaluationResult\>，基础 CRUD Mapper
## 数据库初始化与迁移

- **schema.sql** (classpath 根路径)：DDL 初始化脚本，定义所有业务表（model_config、tool_config、session、message、message_tool_call、session_tool、agent_config、agent_tool、agent_skill、skill_config、skill_tool、session_variable、session_skill、evaluation、evaluation_result）的建表语句和索引
- **SchemaMigration**（com.ghost616.platform.config）：数据库 Schema 迁移组件，继承 ApplicationRunner，通过 ALTER TABLE 实现列增量迁移和 NULL 回填，支持幂等执行和异常跳过；新增 session.is_evaluation 列及 evaluation、evaluation_result 表的全字段迁移