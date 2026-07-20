package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;

import java.util.List;
import java.util.Map;

public interface ContextDataProvider {

    record AgentContextData(Long agentId, String systemPrompt, Long defaultModelId,
                            Integer recentMessageCount, List<SkillConfigDTO> skills,
                            Map<String, String> sessionVariables,
                            Long parentSessionId,
                            List<AgentExecutionContext.ChildSession> childSessions) {
    }

    AgentContextData loadAgentContext(Long sessionId);

    void saveSessionVariable(Long sessionId, String key, String value);

    void deleteSessionVariable(Long sessionId, String key);

    Long createChildSession(Long parentSessionId, String sessionName, String description, Long modelId,
                            List<Long> toolIds, List<Long> skillIds, String prompt);

    List<MessageDataProvider.MessageDTO> getLatestMessages(Long sessionId);

    Map<String, String> getLatestSessionVariables(Long sessionId);

    Map<String, String> getLatestConversationVariables(Long sessionId);

    List<AgentExecutionContext.ChildSession> getLatestChildSessions(Long sessionId);
}
