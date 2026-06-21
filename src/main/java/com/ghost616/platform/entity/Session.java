package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session")
public class Session extends BaseEntity {

    @TableField("agent_id")
    private Long agentId;

    @TableField("model_id")
    private Long modelId;

    private String title;

    @TableField("system_prompt")
    private String systemPrompt;
}
