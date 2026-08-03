package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;

import java.util.List;
import java.util.Map;

public interface ContextDataProvider {

    record AgentContextData(String agentId, String systemPrompt, String defaultModelId,
                            Integer recentMessageCount, List<SkillConfigDTO> skills,
                            Map<String, String> sessionVariables,
                            String parentSessionId,
                            List<AgentExecutionContext.ChildSession> childSessions) {
    }

    AgentContextData loadAgentContext(String sessionId);

    void saveSessionVariable(String sessionId, String key, String value);

    void deleteSessionVariable(String sessionId, String key);

    String createChildSession(String parentSessionId, String sessionName, String description, String modelId,
                              List<String> toolIds, List<String> skillIds, String prompt);

    List<MessageDataProvider.MessageDTO> getLatestMessages(String sessionId);

    Map<String, String> getLatestSessionVariables(String sessionId);

    Map<String, String> getLatestConversationVariables(String sessionId);

    List<AgentExecutionContext.ChildSession> getLatestChildSessions(String sessionId);
}
