package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class AgentExecutionContext {

    private final Long sessionId;
    private final Long agentId;
    private final String systemPrompt;
    private Long modelId;
    private final Integer recentMessageCount;
    @Getter(AccessLevel.NONE)
    private final List<HistoryEntry> history;
    private final List<ToolConfigDTO> tools;
    @Getter(AccessLevel.NONE)
    private final AgentContextMutator mutator;
    @Getter(AccessLevel.NONE)
    private final Map<String, String> sessionVariables;
    @Getter(AccessLevel.NONE)
    private final Map<String, String> conversationVariables;
    private final List<SkillConfigDTO> skills;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public AgentExecutionContext(Long sessionId, Long agentId, String systemPrompt, Long modelId,
                                  Integer recentMessageCount,
                                 List<HistoryEntry> history, List<ToolConfigDTO> tools,
                                 List<SkillConfigDTO> skills,
                                 AgentContextMutator mutator,
                                  Map<String, String> sessionVariables,
                                  Map<String, String> conversationVariables) {
        this.sessionId = sessionId;
        this.agentId = agentId;
        this.systemPrompt = systemPrompt;
        this.modelId = modelId;
        this.recentMessageCount = recentMessageCount;
        this.history = history;
        this.tools = tools;
        this.skills = skills;
        this.mutator = mutator;
        this.sessionVariables = sessionVariables;
        this.conversationVariables = conversationVariables;
        this.mutator.bind(this);
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void putSessionVariable(String key, String value) {
        sessionVariables.put(key, value);
        mutator.putSessionVariable(key, value);
    }

    public void putConversationVariable(String key, String value) {
        conversationVariables.put(key, value);
    }

    public String getSessionVariable(String key) {
        return sessionVariables.get(key);
    }

    public String getConversationVariable(String key) {
        return conversationVariables.get(key);
    }

    public void removeSessionVariable(String key) {
        sessionVariables.remove(key);
        mutator.removeSessionVariable(key);
    }

    public void removeConversationVariable(String key) {
        conversationVariables.remove(key);
    }

    public Set<String> getSessionVariableKeys() {
        return sessionVariables.keySet();
    }

    public Set<String> getConversationVariableKeys() {
        return conversationVariables.keySet();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public record HistoryEntry(String role, String content, String reasoning, String toolCallId,
                               int sequenceNum, LocalDateTime createTime, List<ToolCall> toolCalls) {
    }

    public static class AgentContextMutator {
        private AgentExecutionContext context;
        BiConsumer<String, String> sessionVarPutCallback;
        Consumer<String> sessionVarRemoveCallback;

        public void bind(AgentExecutionContext context) {
            this.context = context;
        }

        public void setModelId(Long modelId) {
            context.modelId = modelId;
        }

        public void addHistoryEntry(HistoryEntry entry) {
            context.history.add(entry);
        }

        public void putSessionVariable(String key, String value) {
            if (sessionVarPutCallback != null) {
                sessionVarPutCallback.accept(key, value);
            }
        }

        public void removeSessionVariable(String key) {
            if (sessionVarRemoveCallback != null) {
                sessionVarRemoveCallback.accept(key);
            }
        }

        public void clearConversationVariables() {
            if (context != null) {
                context.conversationVariables.clear();
            }
        }

        public void setStopped() {
            context.stopped.set(true);
        }

        public void resetStopped() {
            context.stopped.set(false);
        }
    }
}
