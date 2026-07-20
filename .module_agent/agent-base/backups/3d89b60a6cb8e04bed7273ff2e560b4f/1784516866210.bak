package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AgentContextManager {

    private final AgentComponentRegistry registry;
    private ContextDataProvider dataProvider;
    private SessionManager sessionManager;
    private ToolManager toolManager;
    private AgentMessageProxy agentMessageProxy;

    private final ConcurrentHashMap<Long, AgentSessionContext> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized;

    public AgentContextManager(AgentComponentRegistry registry) {
        this.registry = registry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    dataProvider = registry.getContextDataProvider();
                    sessionManager = registry.getSessionManager();
                    toolManager = registry.getToolManager();
                    initialized = true;
                }
            }
        }
    }

    public void setAgentMessageProxy(AgentMessageProxy agentMessageProxy) {
        this.agentMessageProxy = agentMessageProxy;
    }

    public Builder build(Long sessionId) {
        ensureInitialized();
        return new Builder(sessionId);
    }

    public class Builder {
        private final Long sessionId;
        private Long modelIdOverride;

        private Builder(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Builder modelIdOverride(Long modelId) {
            this.modelIdOverride = modelId;
            return this;
        }

        public AgentSessionContext build() {
            AgentSessionContext cached = cache.get(sessionId);
            if (cached != null) {
                return cached;
            }
            AgentSessionContext created = doBuild();
            AgentSessionContext raced = cache.putIfAbsent(sessionId, created);
            return raced != null ? raced : created;
        }

        private AgentSessionContext doBuild() {
            ContextDataProvider.AgentContextData ctxData = dataProvider.loadAgentContext(sessionId);
            if (ctxData == null) {
                throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
            }

            Long agentId = ctxData.agentId();
            String systemPrompt = ctxData.systemPrompt();
            if (systemPrompt == null) {
                systemPrompt = "";
            }

            Long effectiveModelId = (modelIdOverride != null) ? modelIdOverride : ctxData.defaultModelId();

            boolean isSubSession = ctxData.parentSessionId() != null;
            List<ToolConfigDTO> tools = toolManager.getSessionTools(sessionId, isSubSession).stream()
                    .map(ToolManager.ToolSessionObject::toolConfig)
                    .toList();

            List<SkillConfigDTO> skills = ctxData.skills();

            for (SkillConfigDTO skill : skills) {
                if (skill.getSkillTools() != null) {
                    List<ToolConfigDTO> expandedTools = new ArrayList<>();
                    for (ToolConfigDTO tool : skill.getSkillTools()) {
                        if (tool.getToolType() == ToolType.MCP_HTTP && !(tool instanceof McpExpandedToolDTO)) {
                            if (!isSubSession && tool.getSessionAuth() == SessionAuthType.CHILD) {
                                tool.setSessionAuth(SessionAuthType.PARENT);
                                expandedTools.add(tool);
                            } else {
                                List<? extends ToolConfigDTO> mcpTools = toolManager.expandMcpTools(tool);
                                for (ToolConfigDTO mcpTool : mcpTools) {
                                    mcpTool.setSessionAuth(SessionAuthType.PARENT);
                                }
                                expandedTools.addAll(mcpTools);
                            }
                        } else {
                            tool.setSessionAuth(SessionAuthType.PARENT);
                            expandedTools.add(tool);
                        }
                    }
                    skill.setSkillTools(expandedTools);
                }
            }

            List<MessageDataProvider.MessageDTO> messages = sessionManager.getMessages(sessionId);
            List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
            for (MessageDataProvider.MessageDTO msg : messages) {
                List<ToolCall> toolCalls;
                if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                    toolCalls = msg.toolCalls().stream()
                            .map(tc -> ToolCall.builder()
                                    .id(tc.toolCallId())
                                    .name(tc.toolCallName())
                                    .arguments(tc.toolCallArguments())
                                    .build())
                            .toList();
                } else {
                    toolCalls = Collections.emptyList();
                }
                history.add(new AgentExecutionContext.HistoryEntry(
                        msg.role(), msg.content(), msg.reasoning(), msg.toolCallId(),
                        msg.sequenceNum(), msg.createTime(), Collections.unmodifiableList(toolCalls),
                        msg.usage()));
            }

            AgentExecutionContext.AgentContextMutator mutator = new AgentExecutionContext.AgentContextMutator();

            Long parentSessionId = ctxData.parentSessionId();
            AgentSessionContext parentCtx = null;
            if (parentSessionId != null) {
                parentCtx = cache.get(parentSessionId);
                if (parentCtx == null) {
                    parentCtx = AgentContextManager.this.build(parentSessionId).build();
                }
            }

            AgentExecutionContext context = new AgentExecutionContext(
                    sessionId, agentId, systemPrompt, effectiveModelId,
                    ctxData.recentMessageCount(),
                    history, tools, skills, mutator,
                    ctxData.sessionVariables(), new HashMap<>(),
                    parentSessionId, System.getProperty("user.dir"), ctxData.childSessions());

            injectVariableCallbacks(mutator, sessionId, parentSessionId, parentCtx);

            return new AgentSessionContext(context, mutator, new AtomicBoolean(false));
        }

        private void injectVariableCallbacks(AgentExecutionContext.AgentContextMutator mutator,
                                              Long sessionId, Long parentSessionId,
                                              AgentSessionContext parentCtx) {
            if (parentSessionId != null && parentCtx != null) {
                AgentExecutionContext parentContext = parentCtx.context();
                mutator.sessionVarPutCallback = parentContext::putSessionVariable;
                mutator.sessionVarRemoveCallback = parentContext::removeSessionVariable;
                mutator.conversationVarPutCallback = parentContext::putConversationVariable;
                mutator.conversationVarRemoveCallback = parentContext::removeConversationVariable;
                mutator.getSessionVarCallback = parentContext::getSessionVariable;
                mutator.getConversationVarCallback = parentContext::getConversationVariable;
                mutator.getSessionVarKeysCallback = parentContext::getSessionVariableKeys;
                mutator.getConversationVarKeysCallback = parentContext::getConversationVariableKeys;
            } else {
                mutator.sessionVarPutCallback = (key, value) ->
                        dataProvider.saveSessionVariable(sessionId, key, value);
                mutator.sessionVarRemoveCallback = (key) ->
                        dataProvider.deleteSessionVariable(sessionId, key);
                mutator.conversationVarPutCallback = (key, value) ->
                        dataProvider.saveSessionVariable(sessionId, key, value);
                mutator.conversationVarRemoveCallback = (key) ->
                        dataProvider.deleteSessionVariable(sessionId, key);
            }
            mutator.createChildSessionCallback = (psId, sessionName, description, modelId,
                                                    toolIds, skillIds, prompt) ->
                    createChildSession(psId, sessionName, description, modelId,
                            toolIds, skillIds, prompt);
            mutator.sendUserMessageCallback = (childSessionId, content, modelId, thinking) ->
                    sendUserMessage(childSessionId, content, modelId, thinking);

        }
    }

    private Long createChildSession(Long parentSessionId, String sessionName, String description, Long modelId,
                                      List<Long> toolIds, List<Long> skillIds, String prompt) {
        return dataProvider.createChildSession(parentSessionId, sessionName, description, modelId, toolIds, skillIds, prompt);
    }

    private Message sendUserMessage(Long childSessionId, String content, Long modelId, Boolean thinking) {
        if (agentMessageProxy != null) {
            return agentMessageProxy.sendUserMessage(childSessionId, content, modelId, thinking);
        }
        return null;
    }

    public AgentSessionContext get(Long sessionId) {
        ensureInitialized();
        return cache.get(sessionId);
    }

    public void addHistoryEntry(Long sessionId, AgentExecutionContext.HistoryEntry entry) {
        ensureInitialized();
        AgentSessionContext ctx = cache.get(sessionId);
        if (ctx != null) {
            ctx.mutator().addHistoryEntry(entry);
        }
    }

    public void remove(Long sessionId) {
        cache.remove(sessionId);
    }

    public record AgentSessionContext(AgentExecutionContext context,
                                      AgentExecutionContext.AgentContextMutator mutator,
                                      AtomicBoolean toolInvoking) {
    }
}
