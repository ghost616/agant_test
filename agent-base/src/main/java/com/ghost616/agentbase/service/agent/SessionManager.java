package com.ghost616.agentbase.service.agent;

import java.util.List;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;

public class SessionManager {

    private final AgentComponentRegistry registry;
    private MessageDataProvider dataProvider;
    private volatile boolean initialized;

    public SessionManager(AgentComponentRegistry registry) {
        this.registry = registry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    dataProvider = registry.getMessageDataProvider();
                    initialized = true;
                }
            }
        }
    }

    public MessageSaveBuilder messageSave() {
        ensureInitialized();
        return new MessageSaveBuilder();
    }

    public class MessageSaveBuilder {
        private String sessionId;
        private String role;
        private String content;
        private String reasoning;
        private ToolInfo toolInfo;
        private String toolResult;
        private List<MessageDataProvider.ToolCallData> toolCalls;
        private UsageInfo usage;
        private List<MessageDataProvider.WebSearchCallData> webSearchCall;
        private List<MessageDataProvider.CustomToolCallData> customToolCall;

        private MessageSaveBuilder() {
        }

        public MessageSaveBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public MessageSaveBuilder role(String role) {
            this.role = role;
            return this;
        }

        public MessageSaveBuilder content(String content) {
            this.content = content;
            return this;
        }

        public MessageSaveBuilder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public MessageSaveBuilder toolInfo(ToolInfo toolInfo) {
            this.toolInfo = toolInfo;
            return this;
        }

        public MessageSaveBuilder toolResult(String toolResult) {
            this.toolResult = toolResult;
            return this;
        }

        public MessageSaveBuilder toolCalls(List<MessageDataProvider.ToolCallData> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public MessageSaveBuilder usage(UsageInfo usage) {
            this.usage = usage;
            return this;
        }

        public MessageSaveBuilder webSearchCall(List<MessageDataProvider.WebSearchCallData> webSearchCall) {
            this.webSearchCall = webSearchCall;
            return this;
        }

        public MessageSaveBuilder customToolCall(List<MessageDataProvider.CustomToolCallData> customToolCall) {
            this.customToolCall = customToolCall;
            return this;
        }

        public String save() {
            if (sessionId == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空");
            }
            if (role == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "role 不能为空");
            }
            if (content == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
            }
            return dataProvider.saveMessage(sessionId, role, content, reasoning,
                    toolInfo, toolResult, toolCalls, usage, webSearchCall, customToolCall);
        }
    }

    public List<MessageDataProvider.MessageDTO> getMessages(String sessionId) {
        ensureInitialized();
        return dataProvider.getMessages(sessionId);
    }

    public int rollbackToLastUserMessage(String sessionId) {
        ensureInitialized();
        return dataProvider.rollbackToLastUserMessage(sessionId);
    }
}
