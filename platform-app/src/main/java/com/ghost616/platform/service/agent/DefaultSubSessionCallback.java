package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.service.agent.invoker.SubSessionCallback;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class DefaultSubSessionCallback implements SubSessionCallback {

    private final SessionMapper sessionMapper;

    private final ConcurrentHashMap<Long, SubSessionData> subSessionDataMap = new ConcurrentHashMap<>();

    @Override
    public Message execute(Long sessionId, String userMessage, Boolean thinking) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getParentSessionId() == null) {
            return null;
        }
        Long parentSessionId = session.getParentSessionId();

        CompletableFuture<Message> messageResult = new CompletableFuture<>();
        SubSessionData data = new SubSessionData(sessionId, userMessage, thinking, messageResult);
        subSessionDataMap.put(parentSessionId, data);

        try {
            return messageResult.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SubSession execution interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("SubSession execution failed", e.getCause());
        } finally {
            subSessionDataMap.remove(parentSessionId);
        }
    }

    public SubSessionData getSubSessionData(Long parentSessionId) {
        return subSessionDataMap.get(parentSessionId);
    }

    public static class SubSessionData {
        private final Long childSessionId;
        private final String userMessage;
        private final Boolean thinking;
        private final CompletableFuture<Message> messageResult;

        public SubSessionData(Long childSessionId, String userMessage, Boolean thinking, CompletableFuture<Message> messageResult) {
            this.childSessionId = childSessionId;
            this.userMessage = userMessage;
            this.thinking = thinking;
            this.messageResult = messageResult;
        }

        public Long getChildSessionId() {
            return childSessionId;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public Boolean getThinking() {
            return thinking;
        }

        public CompletableFuture<Message> getMessageResult() {
            return messageResult;
        }
    }
}
