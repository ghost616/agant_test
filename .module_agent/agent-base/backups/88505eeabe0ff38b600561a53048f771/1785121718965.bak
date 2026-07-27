package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;

import java.util.List;

public interface ToolDataProvider {

    record SessionToolInfo(String toolId, SessionAuthType sessionAuth) {}

    List<SessionToolInfo> getSessionToolIds(String sessionId);

    ToolConfigDTO getToolById(String toolId);

    List<String> getSkillToolIds(String sessionId);

    CustomToolInvoker getCustomInvoker(ToolConfigDTO toolConfig);
}
