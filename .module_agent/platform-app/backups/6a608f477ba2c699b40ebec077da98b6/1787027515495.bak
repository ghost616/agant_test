package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * WebSocket 推送服务。
 *
 * <p>按会话 ID 定位已绑定的 WebSocket 连接，将消息对象序列化为 JSON 后推送。
 * 无连接或推送失败时静默丢弃并记录 WARN 日志，不影响业务主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final SessionConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    /**
     * 按会话 ID 向该会话的全部已绑定连接推送 JSON 消息。
     *
     * @param sessionId 目标会话 ID
     * @param payload   消息对象，序列化为 JSON 后推送；空会话 ID 或空负载直接忽略
     */
    public void pushToSession(String sessionId, Object payload) {
        if (sessionId == null || sessionId.isBlank() || payload == null) {
            return;
        }
        List<WebSocketSession> sessions = registry.getSessions(sessionId);
        if (sessions.isEmpty()) {
            log.warn("WebSocket 推送丢弃: 会话 {} 无已绑定连接", sessionId);
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket 推送丢弃: 消息序列化失败, sessionId={}, error={}", sessionId, e.getMessage());
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                } else {
                    log.warn("WebSocket 推送丢弃: 会话 {} 的连接 {} 已关闭", sessionId, session.getId());
                }
            } catch (Exception e) {
                log.warn("WebSocket 推送丢弃: 会话 {} 推送失败, error={}", sessionId, e.getMessage());
            }
        }
    }
}