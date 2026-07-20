package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.sendmessage.ChildCreateSession;
import com.ghost616.agentbase.sendmessage.HistoryMessage;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentbase.sendmessage.ChildMessageEvent;
import com.ghost616.agentbase.sendmessage.VariableMessage;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
public class AgentExecutionContext {

    private final Long sessionId;
    private final Long agentId;
    private final String systemPrompt;
    private Long modelId;
    private final Long parentSessionId;
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
    private final String projectDir;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    @Getter(AccessLevel.NONE)
    private final List<ChildSession> childSessions = new ArrayList<>();

    public AgentExecutionContext(Long sessionId, Long agentId, String systemPrompt, Long modelId,
                                  Integer recentMessageCount,
                                 List<HistoryEntry> history, List<ToolConfigDTO> tools,
                                 List<SkillConfigDTO> skills,
                                 AgentContextMutator mutator,
                                  Map<String, String> sessionVariables,
                                  Map<String, String> conversationVariables,
                                  Long parentSessionId, String projectDir, List<ChildSession> childSessions) {
        this.sessionId = sessionId;
        this.agentId = agentId;
        this.systemPrompt = systemPrompt;
        this.modelId = modelId;
        this.parentSessionId = parentSessionId;
        this.recentMessageCount = recentMessageCount;
        this.history = history;
        this.tools = tools;
        this.skills = skills;
        this.mutator = mutator;
        this.sessionVariables = sessionVariables;
        this.conversationVariables = conversationVariables;
        this.projectDir = projectDir;
        if (childSessions != null) {
            this.childSessions.addAll(childSessions);
        }
        this.mutator.bind(this);
    }

    public List<ChildSession> getChildSessions() {
        return Collections.unmodifiableList(childSessions);
    }

    public Long createChildSession(String sessionName, String description, Long modelId,
                                     List<Long> toolIds, List<Long> skillIds, String prompt) {
        Long childSessionId = mutator.createChildSession(sessionName, description, modelId, toolIds, skillIds, prompt);
        if (childSessionId != null) {
            childSessions.add(new ChildSession(childSessionId, sessionName, description, modelId));
        }
        return childSessionId;
    }

    public Message sendUserMessage(Long childSessionId, String content, Long modelId, Boolean thinking) {
        return mutator.sendUserMessage(childSessionId, content, modelId, thinking);
    }

    public record ChildSession(Long sessionId, String sessionName, String description, Long modelId) {
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void putSessionVariable(String key, String value) {
        if (parentSessionId != null) {
            mutator.putSessionVariable(key, value);
            return;
        }
        sessionVariables.put(key, value);
        mutator.putSessionVariable(key, value);
    }

    public void putConversationVariable(String key, String value) {
        if (parentSessionId != null) {
            mutator.putConversationVariable(key, value);
            return;
        }
        conversationVariables.put(key, value);
        mutator.putConversationVariable(key, value);
    }

    public String getSessionVariable(String key) {
        if (parentSessionId != null) {
            return mutator.getSessionVariable(key);
        }
        return sessionVariables.get(key);
    }

    public String getConversationVariable(String key) {
        if (parentSessionId != null) {
            return mutator.getConversationVariable(key);
        }
        return conversationVariables.get(key);
    }

    public void removeSessionVariable(String key) {
        if (parentSessionId != null) {
            mutator.removeSessionVariable(key);
            return;
        }
        sessionVariables.remove(key);
        mutator.removeSessionVariable(key);
    }

    public void removeConversationVariable(String key) {
        if (parentSessionId != null) {
            mutator.removeConversationVariable(key);
            return;
        }
        conversationVariables.remove(key);
        mutator.removeConversationVariable(key);
    }

    public Set<String> getSessionVariableKeys() {
        if (parentSessionId != null) {
            return mutator.getSessionVariableKeys();
        }
        return sessionVariables.keySet();
    }

    public Set<String> getConversationVariableKeys() {
        if (parentSessionId != null) {
            return mutator.getConversationVariableKeys();
        }
        return conversationVariables.keySet();
    }

    /**
     * 判断当前会话是否为主会话。
     *
     * @return 若 parentSessionId 为 null 则返回 true（主会话），否则返回 false（子会话）
     */
    public boolean isMainSession() {
        return parentSessionId == null;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public record HistoryEntry(String role, String content, String reasoning, String toolCallId,
                               int sequenceNum, LocalDateTime createTime, List<ToolCall> toolCalls,
                               UsageInfo usage) {
    }

    public static class AgentContextMutator {
        private AgentExecutionContext context;
        BiConsumer<String, String> sessionVarPutCallback;
        Consumer<String> sessionVarRemoveCallback;
        BiConsumer<String, String> conversationVarPutCallback;
        Consumer<String> conversationVarRemoveCallback;
        Function<String, String> getSessionVarCallback;
        Function<String, String> getConversationVarCallback;
        Supplier<Set<String>> getSessionVarKeysCallback;
        Supplier<Set<String>> getConversationVarKeysCallback;
        CreateChildSessionCallback createChildSessionCallback;
        SendUserMessageCallback sendUserMessageCallback;
        private MessageSender messageSender;

        @FunctionalInterface
        public interface CreateChildSessionCallback {
            Long create(Long parentSessionId, String sessionName, String description, Long modelId,
                        List<Long> toolIds, List<Long> skillIds, String prompt);
        }

        @FunctionalInterface
        public interface SendUserMessageCallback {
            Message send(Long childSessionId, String content, Long modelId, Boolean thinking);
        }

        public void bind(AgentExecutionContext context) {
            this.context = context;
        }

        public void setModelId(Long modelId) {
            context.modelId = modelId;
        }

        public void addHistoryEntry(HistoryEntry entry) {
            context.history.add(entry);
            if (messageSender != null) {
                messageSender.send(new HistoryMessage(context.sessionId, entry));
            }
        }

        public void putSessionVariable(String key, String value) {
            if (sessionVarPutCallback != null) {
                sessionVarPutCallback.accept(key, value);
            }
            if (messageSender != null) {
                messageSender.send(new VariableMessage(context.sessionId, "SESSION", key, value, "PUT"));
            }
        }

        public void removeSessionVariable(String key) {
            if (sessionVarRemoveCallback != null) {
                sessionVarRemoveCallback.accept(key);
            }
            if (messageSender != null) {
                messageSender.send(new VariableMessage(context.sessionId, "SESSION", key, null, "REMOVE"));
            }
        }

        public void putConversationVariable(String key, String value) {
            if (conversationVarPutCallback != null) {
                conversationVarPutCallback.accept(key, value);
            }
            if (messageSender != null) {
                messageSender.send(new VariableMessage(context.sessionId, "CONVERSATION", key, value, "PUT"));
            }
        }

        public void removeConversationVariable(String key) {
            if (conversationVarRemoveCallback != null) {
                conversationVarRemoveCallback.accept(key);
            }
            if (messageSender != null) {
                messageSender.send(new VariableMessage(context.sessionId, "CONVERSATION", key, null, "REMOVE"));
            }
        }

        public String getSessionVariable(String key) {
            if (getSessionVarCallback != null) {
                return getSessionVarCallback.apply(key);
            }
            return null;
        }

        public String getConversationVariable(String key) {
            if (getConversationVarCallback != null) {
                return getConversationVarCallback.apply(key);
            }
            return null;
        }

        public Set<String> getSessionVariableKeys() {
            if (getSessionVarKeysCallback != null) {
                return getSessionVarKeysCallback.get();
            }
            return Collections.emptySet();
        }

        public Set<String> getConversationVariableKeys() {
            if (getConversationVarKeysCallback != null) {
                return getConversationVarKeysCallback.get();
            }
            return Collections.emptySet();
        }

        public void refreshHistory(List<HistoryEntry> history) {
            context.history.clear();
            context.history.addAll(history);
        }

        public void refreshSessionVariables(Map<String, String> vars) {
            context.sessionVariables.clear();
            if (vars != null) {
                context.sessionVariables.putAll(vars);
            }
        }

        public void refreshConversationVariables(Map<String, String> vars) {
            context.conversationVariables.clear();
            if (vars != null) {
                context.conversationVariables.putAll(vars);
            }
        }

        public void refreshChildSessions(List<ChildSession> children) {
            context.childSessions.clear();
            if (children != null) {
                context.childSessions.addAll(children);
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

        public Long createChildSession(String sessionName, String description, Long modelId,
                                          List<Long> toolIds, List<Long> skillIds, String prompt) {
            if (context.parentSessionId != null) {
                return null;
            }
            if (sessionName == null || sessionName.isBlank()) {
                return null;
            }
            if (modelId == null) {
                modelId = context.modelId;
            }
            Long childSessionId = null;
            if (createChildSessionCallback != null) {
                childSessionId = createChildSessionCallback.create(context.sessionId, sessionName, description, modelId,
                        toolIds, skillIds, prompt);
            }
            if (childSessionId != null && messageSender != null) {
                messageSender.send(new ChildCreateSession(context.sessionId,
                        new ChildSession(childSessionId, sessionName, description, modelId)));
            }
            return childSessionId;
        }

        public Message sendUserMessage(Long childSessionId, String content, Long modelId, Boolean thinking) {
            Message result = null;
            if (sendUserMessageCallback != null) {
                result = sendUserMessageCallback.send(childSessionId, content, modelId, thinking);
            }
            if (messageSender != null) {
                messageSender.send(new ChildMessageEvent(childSessionId, childSessionId, content, modelId, thinking, result));
            }
            return result;
        }

        public void setMessageSender(MessageSender messageSender) {
            this.messageSender = messageSender;
        }
    }
}
