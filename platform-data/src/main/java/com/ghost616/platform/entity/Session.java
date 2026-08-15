package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session")
public class Session extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("model_id")
    private Long modelId;

    private String title;

    @TableField("system_prompt")
    private String systemPrompt;

    @TableField("parent_session_id")
    private Long parentSessionId;

    @TableField("is_child")
    private Boolean isChild;

    private String description;

    @TableField("total_token_used")
    private Long totalTokenUsed;

    @TableField("last_response_id")
    private String lastResponseId;

    @TableField("is_evaluation")
    private Boolean isEvaluation;

    @TableField("thinking")
    private Boolean thinking;

    @TableField("memory_point_sequence_num")
    private Integer memoryPointSequenceNum;

    @TableField("memory_prompt")
    private String memoryPrompt;
}
