package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.enums.SessionAuthType;

public record AgentToolItem(
    @JsonSerialize(using = ToStringSerializer.class)
    Long toolId,
    SessionAuthType sessionAuth
) {
}
