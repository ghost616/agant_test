package com.ghost616.platform.dto.agent_evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentEvaluationCreateRequest {

    @NotBlank(message = "评估名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "智能体ID不能为空")
    private Long agentId;
}
