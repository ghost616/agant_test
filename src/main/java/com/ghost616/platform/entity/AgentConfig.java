package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ghost616.platform.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_config")
public class AgentConfig extends BaseEntity {

    private String name;

    private String description;

    @TableField("system_prompt")
    private String systemPrompt;

    @TableField("model_id")
    private Long modelId;

    private CommonStatus status;

    @TableField("recent_message_count")
    private Integer recentMessageCount = 10;
}
