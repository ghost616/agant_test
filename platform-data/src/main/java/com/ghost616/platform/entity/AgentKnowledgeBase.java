package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 智能体-知识库关联实体，映射 agent_knowledge_base 表。
 */
@Data
@TableName("agent_knowledge_base")
public class AgentKnowledgeBase {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;
}
