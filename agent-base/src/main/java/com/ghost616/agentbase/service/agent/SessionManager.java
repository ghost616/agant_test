package com.ghost616.agentbase.service.agent;

import java.util.List;

import com.ghost616.agentbase.core.AgentComponentRegistry;
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
        private Long sessionId;
        private String role;
        private String content;
        private String reasoning;
        private String toolCallId;
        private String toolResult;
        private List<MessageDataProvider.ToolCallData> toolCalls;
        private UsageInfo usage;

        private MessageSaveBuilder() {
        }

        public MessageSaveBuilder sessionId(Long sessionId) {
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

        public MessageSaveBuilder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
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

        public Long save() {
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
                    toolCallId, toolResult, toolCalls, usage);
        }
    }

    public List<MessageDataProvider.MessageDTO> getMessages(Long sessionId) {
        ensureInitialized();
        return dataProvider.getMessages(sessionId);
    }

    public int rollbackToLastUserMessage(Long sessionId) {
        ensureInitialized();
        return dataProvider.rollbackToLastUserMessage(sessionId);
    }
}
