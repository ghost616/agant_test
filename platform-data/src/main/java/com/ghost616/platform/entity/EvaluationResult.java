package com.ghost616.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("evaluation_result")
public class EvaluationResult extends BaseEntity {

    @TableField("evaluation_id")
    private Long evaluationId;

    @TableField("evaluation_session_id")
    private Long evaluationSessionId;

    @TableField("result")
    private String result;

    @TableField("execution_status")
    private String executionStatus = "PENDING";

    @TableField("model_id")
    private Long modelId;

    @TableField("final_score")
    private Integer finalScore;
}
