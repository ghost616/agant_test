package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("evaluation")
public class Evaluation extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("benchmark_session_id")
    private Long benchmarkSessionId;

    @TableField("execution_count")
    private Integer executionCount;

    @TableField("model_id")
    private Long modelId;

    @TableField("agent_eval_id")
    private Long agentEvalId;

    @TableField("agent_id")
    private Long agentId;

    @TableField("execution_type")
    private String executionType = "BACKGROUND";
}
