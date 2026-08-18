package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.sendmessage.MessageDefinition;
import com.ghost616.agentbase.sendmessage.MessageName;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentbase.sendmessage.SendUserMessage;
import com.ghost616.platform.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * agent-base {@link MessageSender} 默认实现（Spring Bean）。
 *
 * <p>收到消息后在独立线程中按消息类型（{@link MessageDefinition#getMessageName()}）
 * 分发到不同业务流程。当前仅实现 SEND_USER_MESSAGE 流程：
 * 将消息经 {@link WebSocketPushService} 通过 WebSocket 推送到前端。
 * 推送目标同时覆盖主会话（前端主界面绑定）与子会话本身（子会话详情页绑定）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMessageSender implements MessageSender {

    private final WebSocketPushService pushService;

    @Override
    public void send(MessageDefinition message) {
        if (message == null) {
            return;
        }
        CompletableFuture.runAsync(() -> dispatch(message));
    }

    /**
     * 按消息类型分发到对应业务流程。
     */
    private void dispatch(MessageDefinition message) {
        try {
            switch (message.getMessageName()) {
                case MessageName.SEND_USER_MESSAGE -> handleSendUserMessage((SendUserMessage) message);
                default -> log.debug("MessageSender 暂不支持的消息类型: {}", message.getMessageName());
            }
        } catch (Exception e) {
            log.warn("MessageSender 分发消息失败, type={}, error={}", message.getMessageName(), e.getMessage(), e);
        }
    }

    /**
     * SEND_USER_MESSAGE 流程：将用户消息推送至前端。
     *
     * <p>主会话与子会话 ID 不同时分别推送，同一连接绑定多个会话时由前端按
     * 会话 ID 过滤，推送服务内部对无连接场景静默丢弃。</p>
     */
    private void handleSendUserMessage(SendUserMessage message) {
        if (isNotBlank(message.getMainSessionId())) {
            pushService.pushToSession(message.getMainSessionId(), message);
        }
        if (isNotBlank(message.getSessionId()) && !message.getSessionId().equals(message.getMainSessionId())) {
            pushService.pushToSession(message.getSessionId(), message);
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}