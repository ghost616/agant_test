package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;

import java.util.List;
import java.util.Map;

public interface ContextDataProvider {

    Long getAgentId(Long sessionId);

    String getSystemPrompt(Long agentId);

    Long getDefaultModelId(Long agentId);

    Integer getRecentMessageCount(Long agentId);

    List<SkillConfigDTO> loadSkills(Long agentId);

    Map<String, String> loadSessionVariables(Long sessionId);

    void saveSessionVariable(Long sessionId, String key, String value);

    void deleteSessionVariable(Long sessionId, String key);
}
