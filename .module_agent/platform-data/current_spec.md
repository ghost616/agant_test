# platform-data 模块功能说明

> 待力牧首次执行后填充，记录模块公共方法与功能。
## 数据实体层

- **SubToolType**：子工具类型枚举（BROWSER/RAG_KNOWLEDGE），@EnumValue 标记 code 字段
- **AgentLogEntity**：智能体日志实体，继承 BaseEntity，映射 agent_log 表，含 sessionId(会话ID)/conversationId(对话ID)/logType(日志类型，存储 LogType 枚举 code 值)/logLevel(日志等级，存储 LogLevel 枚举 code 值)/logData(LogData 对象序列化 JSON 文本) 字段
## 数据访问层

提供 16 个 MyBatis-Plus Mapper 接口，均位于 `com.ghost616.platform.repository` 包下：
- **MessageMapper**：继承 BaseMapper\<Message\>，额外提供 rollbackBySessionIdAndGeSequenceNum 批量更新方法、selectByConversationId 按会话查询未回滚消息（按创建时间升序）
- **MessageToolCallMapper**：继承 BaseMapper\<MessageToolCall\>，额外提供 deleteByMessageIds 批量删除方法
- **SessionMapper**：继承 BaseMapper\<Session\>，额外提供 addTotalTokenUsed 原子增减方法
- **AgentConfigMapper、AgentToolMapper、AgentSkillMapper**：基础 CRUD Mapper
- **ModelConfigMapper、ToolConfigMapper**：基础 CRUD Mapper
- **SkillConfigMapper、SkillToolMapper**：基础 CRUD Mapper
- **SessionToolMapper、SessionVariableMapper、SessionSkillMapper**：基础 CRUD Mapper
- **EvaluationMapper**：继承 BaseMapper\<Evaluation\>，基础 CRUD Mapper
- **EvaluationResultMapper**：继承 BaseMapper\<EvaluationResult\>，基础 CRUD Mapper
- **AgentEvaluationMapper**：继承 BaseMapper\<AgentEvaluation\>，基础 CRUD Mapper
- **AgentLogMapper**：继承 BaseMapper\<AgentLogEntity\>，基础 CRUD Mapper
知识库相关 Mapper：
- **KnowledgeBaseMapper**：继承 BaseMapper\<KnowledgeBase\>，基础 CRUD Mapper
- **KnowledgeFileMapper**：继承 BaseMapper\<KnowledgeFile\>，基础 CRUD Mapper
- **AgentKnowledgeBaseMapper**：继承 BaseMapper\<AgentKnowledgeBase\>，基础 CRUD Mapper
## 数据库初始化与迁移

- **schema.sql** (classpath 根路径)：DDL 初始化脚本，定义所有业务表（model_config、tool_config、session、message、message_tool_call、session_tool、agent_config、agent_tool、agent_skill、skill_config、skill_tool、session_variable、session_skill、evaluation、evaluation_result）的建表语句和索引
- **SchemaMigration**（com.ghost616.platform.config）：数据库 Schema 迁移组件，继承 ApplicationRunner，通过 ALTER TABLE 实现列增量迁移和 NULL 回填，支持幂等执行和异常跳过；新增 session.is_evaluation 列及 evaluation、evaluation_result 表的全字段迁移
新增 agent_evaluation 表建表语句及 evaluation 表 agent_eval_id/agent_id/execution_type 列；SchemaMigration 新增 agent_evaluation 表全字段迁移及 evaluation.agent_eval_id/evaluation.agent_id/evaluation.execution_type 迁移条目
新增 knowledge_base、knowledge_file、agent_knowledge_base 三张表建表语句及对应索引；SchemaMigration 新增三张表全字段迁移条目
SchemaMigration 新增 knowledge_file.publish_status（VARCHAR(32)，默认 'UNPUBLISHED'）、knowledge_base.vector_model_id（BIGINT）、knowledge_base.es_index（VARCHAR(255)）、knowledge_base.rebuilding（TINYINT(1)，默认 0）四个列迁移条目；schema.sql knowledge_base/knowledge_file 表 DDL 同步新增对应列
新增 agent_log 表建表语句及 session_id/conversation_id 索引；schema.sql 定义的表新增 agent_log（智能体日志，含 session_id/conversation_id/log_type/log_level/log_data）