package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.enums.SessionAuthType;

public record AgentSkillItem(
    @JsonSerialize(using = ToStringSerializer.class)
    Long skillId,
    SessionAuthType sessionAuth
) {
}
