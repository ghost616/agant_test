package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体日志实体，映射 agent_log 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_log")
public class AgentLogEntity extends BaseEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("log_type")
    private String logType;

    @TableField("log_level")
    private String logLevel;

    @TableField("log_data")
    private String logData;
}
