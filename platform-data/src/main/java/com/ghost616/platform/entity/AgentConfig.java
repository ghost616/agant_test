package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.SubSessionOpenMode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_config")
public class AgentConfig extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String name;

    private String description;

    @TableField("system_prompt")
    private String systemPrompt;

    @TableField("model_id")
    private Long modelId;

    private CommonStatus status;

    @TableField("recent_message_count")
    private Integer recentMessageCount = 10;

    @TableField("memory_enabled")
    private Boolean memoryEnabled = false;

    @TableField("memory_group_count")
    private Integer memoryGroupCount = 30;

    @TableField("vector_model_id")
    private Long vectorModelId;

    @TableField("sub_session_open_mode")
    private SubSessionOpenMode subSessionOpenMode = SubSessionOpenMode.DEFAULT;
}
