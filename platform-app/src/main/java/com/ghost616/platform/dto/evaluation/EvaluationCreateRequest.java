package com.ghost616.platform.dto.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationCreateRequest {

    @NotBlank(message = "评估名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotNull(message = "基准会话ID不能为空")
    private Long benchmarkSessionId;

    @NotNull(message = "执行次数不能为空")
    private Integer executionCount;
}
