package com.ghost616.platform.dto;

import lombok.Data;

@Data
public class AgentContextBasicDTO {

    private Long sessionId;
    private Long agentId;
    private Long modelId;
    private String lastResponseId;
    private Long parentSessionId;
}
