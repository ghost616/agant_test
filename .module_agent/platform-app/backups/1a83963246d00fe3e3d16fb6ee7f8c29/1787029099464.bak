package com.ghost616.platform.websocket;

import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 用户会话 WebSocket 处理器。
 *
 * <p>连接建立时从握手属性取 {@link UserSession}（由 {@link AuthHandshakeInterceptor} 写入），
 * 自动以用户会话 ID（{@link UserSession#getSessionId()}）委托 {@link SessionConnectionRegistry}
 * 注册连接（用户会话级绑定）；握手属性无 UserSession 时不绑定并记录 WARN。
 * 连接关闭时清理该连接的全部注册。不再处理 BIND/UNBIND 入站绑定消息，
 * 入站文本消息由 {@link TextWebSocketHandler} 默认空实现静默忽略。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private final SessionConnectionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UserSession userSession = (UserSession) session.getAttributes()
                .get(AuthHandshakeInterceptor.USER_SESSION_ATTR);
        if (userSession == null) {
            log.warn("WebSocket 连接建立但握手属性无 UserSession, 不绑定用户会话: session={}", session.getId());
            return;
        }
        registry.bind(userSession.getSessionId(), session);
        log.debug("WebSocket 连接建立并绑定用户会话: session={}, userSessionId={}",
                session.getId(), userSession.getSessionId());
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
}
