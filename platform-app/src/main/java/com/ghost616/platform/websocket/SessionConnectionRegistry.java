package com.ghost616.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 用户会话连接注册中心。
 *
 * <p>维护「用户会话 ID → WebSocket 连接集合」的映射（同一用户会话允许多个连接，如多标签页），
 * 并维护「连接 → 已绑定用户会话 ID 集合」的反向索引，用于连接关闭时清理注册。
 * 绑定基于用户会话 ID（{@link com.ghost616.platform.session.UserSession#getSessionId()}），
 * 仅校验连接有效性后做纯内存注册，不查询会话表、不做属主校验（用户会话级绑定）。</p>
 */
@Slf4j
@Component
public class SessionConnectionRegistry {

    /** 用户会话 ID → 连接集合。 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> connections = new ConcurrentHashMap<>();

    /** 连接 → 已绑定用户会话 ID 集合（反向索引，连接关闭时清理）。 */
    private final ConcurrentHashMap<WebSocketSession, CopyOnWriteArraySet<String>> boundSessionIds = new ConcurrentHashMap<>();

    /**
     * 绑定结果。
     *
     * @param success 是否成功
     * @param message 失败原因，成功时为 null
     */
    public record BindResult(boolean success, String message) {

        public static BindResult ok() {
            return new BindResult(true, null);
        }

        public static BindResult fail(String message) {
            return new BindResult(false, message);
        }
    }

    /**
     * 绑定连接与用户会话。
     *
     * <p>仅校验连接有效性后做纯内存注册，不查询会话表、不做属主校验。
     * 同一用户会话允许多个连接（如多标签页）。</p>
     *
     * @param userSessionId 用户会话 ID（{@link com.ghost616.platform.session.UserSession#getSessionId()}）
     * @param session       WebSocket 连接
     * @return 绑定结果，连接无效或用户会话 ID 无效时绑定失败
     */
    public BindResult bind(String userSessionId, WebSocketSession session) {
        if (session == null) {
            return BindResult.fail("连接无效");
        }
        if (userSessionId == null || userSessionId.isBlank()) {
            return BindResult.fail("用户会话 ID 无效");
        }
        connections.computeIfAbsent(userSessionId, k -> new CopyOnWriteArraySet<>()).add(session);
        boundSessionIds.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>()).add(userSessionId);
        log.debug("WebSocket 连接绑定用户会话: session={}, userSessionId={}", session.getId(), userSessionId);
        return BindResult.ok();
    }

    /**
     * 解绑连接与用户会话。
     *
     * @param userSessionId 用户会话 ID
     * @param session       WebSocket 连接
     */
    public void unbind(String userSessionId, WebSocketSession session) {
        Set<WebSocketSession> sessionConnections = connections.get(userSessionId);
        if (sessionConnections != null) {
            sessionConnections.remove(session);
            if (sessionConnections.isEmpty()) {
                connections.remove(userSessionId, sessionConnections);
            }
        }
        Set<String> boundSessions = boundSessionIds.get(session);
        if (boundSessions != null) {
            boundSessions.remove(userSessionId);
            if (boundSessions.isEmpty()) {
                boundSessionIds.remove(session, boundSessions);
            }
        }
    }

    /**
     * 获取指定用户会话的全部连接快照。
     *
     * @param userSessionId 用户会话 ID
     * @return 连接列表，无绑定时返回空列表
     */
    public List<WebSocketSession> getSessions(String userSessionId) {
        if (userSessionId == null || userSessionId.isBlank()) {
            return List.of();
        }
        Set<WebSocketSession> sessionConnections = connections.get(userSessionId);
        if (sessionConnections == null || sessionConnections.isEmpty()) {
            return List.of();
        }
        return List.copyOf(sessionConnections);
    }

    /**
     * 连接关闭时清理该连接注册的所有用户会话绑定。
     *
     * @param session 已关闭的连接
     */
    public void removeAll(WebSocketSession session) {
        Set<String> boundSessions = boundSessionIds.remove(session);
        if (boundSessions == null) {
            return;
        }
        for (String userSessionId : boundSessions) {
            Set<WebSocketSession> sessionConnections = connections.get(userSessionId);
            if (sessionConnections != null) {
                sessionConnections.remove(session);
                if (sessionConnections.isEmpty()) {
                    connections.remove(userSessionId, sessionConnections);
                }
            }
        }
        log.debug("WebSocket 连接关闭, 清理 {} 个用户会话绑定", boundSessions.size());
    }
}
