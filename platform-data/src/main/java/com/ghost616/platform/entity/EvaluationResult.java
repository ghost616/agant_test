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
}
