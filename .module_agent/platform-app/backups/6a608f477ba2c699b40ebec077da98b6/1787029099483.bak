package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * WebSocket 推送服务。
 *
 * <p>从 {@link UserContext} 获取当前用户会话，以用户会话 ID 定位已绑定的 WebSocket 连接，
 * 将消息对象序列化为 JSON 后推送。无当前用户上下文、无连接或推送失败时静默丢弃并记录
 * WARN 日志，不影响业务主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final SessionConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    /**
     * 向当前用户会话的全部已绑定连接推送 JSON 消息。
     *
     * @param payload 消息对象，序列化为 JSON 后推送；空负载或当前线程无用户上下文时直接忽略
     */
    public void pushToSession(Object payload) {
        if (payload == null) {
            return;
        }
        UserSession userSession = UserContext.get();
        if (userSession == null) {
            log.warn("WebSocket 推送丢弃: 当前线程无用户会话上下文");
            return;
        }
        String userSessionId = userSession.getSessionId();
        if (userSessionId == null || userSessionId.isBlank()) {
            log.warn("WebSocket 推送丢弃: 用户会话 ID 无效");
            return;
        }
        List<WebSocketSession> sessions = registry.getSessions(userSessionId);
        if (sessions.isEmpty()) {
            log.warn("WebSocket 推送丢弃: 用户会话 {} 无已绑定连接", userSessionId);
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket 推送丢弃: 消息序列化失败, userSessionId={}, error={}", userSessionId, e.getMessage());
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                } else {
                    log.warn("WebSocket 推送丢弃: 用户会话 {} 的连接 {} 已关闭", userSessionId, session.getId());
                }
            } catch (Exception e) {
                log.warn("WebSocket 推送丢弃: 用户会话 {} 推送失败, error={}", userSessionId, e.getMessage());
            }
        }
    }
}
