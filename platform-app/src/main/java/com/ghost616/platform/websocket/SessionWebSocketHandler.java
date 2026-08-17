package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话 WebSocket 处理器。
 *
 * <p>处理前端绑定/解绑会话消息（{@code {"type":"BIND"|"UNBIND","sessionId":"..."}}），
 * 委托 {@link SessionConnectionRegistry} 维护注册；连接关闭时清理该连接的全部注册。
 * 绑定结果通过回执消息返回前端（{@code messageName: "BIND_RESULT"}）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWebSocketHandler extends TextWebSocketHandler {

    /** 入站消息类型字段。 */
    private static final String FIELD_TYPE = "type";
    /** 入站消息会话 ID 字段。 */
    private static final String FIELD_SESSION_ID = "sessionId";
    /** 绑定会话消息类型。 */
    public static final String TYPE_BIND = "BIND";
    /** 解绑会话消息类型。 */
    public static final String TYPE_UNBIND = "UNBIND";
    /** 绑定结果回执消息名。 */
    public static final String MESSAGE_NAME_BIND_RESULT = "BIND_RESULT";

    private final SessionConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path(FIELD_TYPE).asText();
            String sessionId = node.path(FIELD_SESSION_ID).asText(null);
            if (TYPE_BIND.equals(type)) {
                handleBind(session, sessionId);
            } else if (TYPE_UNBIND.equals(type)) {
                handleUnbind(session, sessionId);
            } else {
                log.warn("WebSocket 收到未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.warn("WebSocket 消息解析失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.removeAll(session);
        log.debug("WebSocket 连接关闭: {}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常: session={}, error={}", session.getId(), exception.getMessage());
    }

    /**
     * 处理绑定消息：从握手属性取当前登录用户，委托注册中心绑定并回执结果。
     */
    private void handleBind(WebSocketSession session, String sessionId) {
        UserSession userSession = (UserSession) session.getAttributes()
                .get(AuthHandshakeInterceptor.USER_SESSION_ATTR);
        Long userId = userSession != null ? userSession.getUser().getId() : null;
        SessionConnectionRegistry.BindResult result = registry.bind(userId, sessionId, session);
        sendBindResult(session, sessionId, result.success(), result.message());
    }

    /**
     * 处理解绑消息。
     */
    private void handleUnbind(WebSocketSession session, String sessionId) {
        registry.unbind(sessionId, session);
    }

    /**
     * 发送绑定结果回执。
     */
    private void sendBindResult(WebSocketSession session, String sessionId,
                                boolean success, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageName", MESSAGE_NAME_BIND_RESULT);
        payload.put(FIELD_SESSION_ID, sessionId);
        payload.put("success", success);
        payload.put("message", message);
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("WebSocket 绑定回执发送失败: session={}, error={}", session.getId(), e.getMessage());
        }
    }
}