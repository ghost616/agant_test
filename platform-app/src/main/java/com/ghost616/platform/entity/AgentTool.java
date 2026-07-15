package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.enums.SessionAuthType;
import lombok.Data;

@Data
@TableName("agent_tool")
public class AgentTool {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("tool_id")
    private Long toolId;

    @TableField("session_auth")
    private SessionAuthType sessionAuth;
}
