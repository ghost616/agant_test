package com.ghost616.platform.dto.agent_evaluation;

import lombok.Data;

@Data
public class AgentEvaluationUpdateRequest {

    private String name;

    private String description;

    private Long agentId;
}
