package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.User;
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
 * <p>提供两个推送入口：</p>
 * <ul>
 *   <li>{@link #pushToSession(Object)}：从 {@link UserContext} 获取当前用户会话，
 *       按其 userId 经 {@link SessionConnectionRegistry#getSessionsByUser(Long)}
 *       获取该用户全部用户会话的所有连接逐个推送（SEND_USER_MESSAGE 默认广播流程）；</li>
 *   <li>{@link #pushToUserSession(String, Object)}：按用户会话 ID 经
 *       {@link SessionConnectionRegistry#getSessions(String)} 精准推送单个客户端。</li>
 * </ul>
 *
 * <p>两个入口共用消息序列化与逐连接发送逻辑，消息对象序列化为 JSON 后推送。
 * 无当前用户上下文、无连接或推送失败时静默丢弃并记录 WARN 日志，不影响业务主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final SessionConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    /**
     * 向当前用户的全部用户会话的所有已绑定连接广播推送 JSON 消息。
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
        User user = userSession.getUser();
        if (user == null || user.getId() == null) {
            log.warn("WebSocket 推送丢弃: 当前用户会话无用户信息");
            return;
        }
        List<WebSocketSession> sessions = registry.getSessionsByUser(user.getId());
        if (sessions.isEmpty()) {
            log.warn("WebSocket 推送丢弃: 用户 {} 无已绑定连接", user.getId());
            return;
        }
        sendToSessions(sessions, payload, "用户 " + user.getId());
    }

    /**
     * 按用户会话 ID 向指定用户会话的全部已绑定连接精准推送 JSON 消息。
     *
     * @param userSessionId 用户会话 ID，无效时静默丢弃
     * @param payload       消息对象，序列化为 JSON 后推送；空负载直接忽略
     */
    public void pushToUserSession(String userSessionId, Object payload) {
        if (payload == null) {
            return;
        }
        if (userSessionId == null || userSessionId.isBlank()) {
            log.warn("WebSocket 推送丢弃: 用户会话 ID 无效");
            return;
        }
        List<WebSocketSession> sessions = registry.getSessions(userSessionId);
        if (sessions.isEmpty()) {
            log.warn("WebSocket 推送丢弃: 用户会话 {} 无已绑定连接", userSessionId);
            return;
        }
        sendToSessions(sessions, payload, "用户会话 " + userSessionId);
    }

    /**
     * 序列化负载并逐个连接推送（两个入口共用的发送逻辑）。
     */
    private void sendToSessions(List<WebSocketSession> sessions, Object payload, String target) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket 推送丢弃: 消息序列化失败, target={}, error={}", target, e.getMessage());
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                } else {
                    log.warn("WebSocket 推送丢弃: {} 的连接 {} 已关闭", target, session.getId());
                }
            } catch (Exception e) {
                log.warn("WebSocket 推送丢弃: {} 推送失败, error={}", target, e.getMessage());
            }
        }
    }
}
