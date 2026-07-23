package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;

import java.util.List;

public interface ToolDataProvider {

    record SessionToolInfo(Long toolId, SessionAuthType sessionAuth) {}

    List<SessionToolInfo> getSessionToolIds(Long sessionId);

    ToolConfigDTO getToolById(Long toolId);

    List<Long> getSkillToolIds(Long sessionId);
}
