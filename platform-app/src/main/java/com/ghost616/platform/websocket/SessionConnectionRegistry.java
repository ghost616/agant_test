package com.ghost616.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 用户会话连接注册中心。
 *
 * <p>基于「用户 ID + 用户会话 ID」双维度维护 WebSocket 连接注册，包含三个索引：</p>
 * <ul>
 *   <li>connections：用户会话 ID → 连接集合（主索引，同一用户会话允许多个连接，如多标签页）</li>
 *   <li>boundUserSessionIds：连接 → 用户会话 ID（反向索引，连接关闭时清理）</li>
 *   <li>userSessionsByUser：用户 ID → 用户会话 ID 集合（userId 维度索引，用于按用户广播）</li>
 * </ul>
 *
 * <p>绑定基于用户会话 ID（{@link com.ghost616.platform.session.UserSession#getSessionId()}）与
 * 用户 ID（{@link com.ghost616.platform.session.UserSession#getUser()}），仅校验参数有效性后
 * 做纯内存注册，不查询会话表、不做属主校验（用户会话级绑定）。</p>
 */
@Slf4j
@Component
public class SessionConnectionRegistry {

    /** 用户会话 ID → 连接集合（主索引）。 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> connections = new ConcurrentHashMap<>();

    /** 连接 → 用户会话 ID（反向索引，连接关闭时清理）。 */
    private final ConcurrentHashMap<WebSocketSession, String> boundUserSessionIds = new ConcurrentHashMap<>();

    /** 用户 ID → 用户会话 ID 集合（userId 维度索引，广播用）。 */
    private final ConcurrentHashMap<Long, Set<String>> userSessionsByUser = new ConcurrentHashMap<>();

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
     * 绑定连接与用户（用户 ID + 用户会话 ID 双维度）。
     *
     * <p>同时更新三个索引：connections 主索引、boundUserSessionIds 反向索引、
     * userSessionsByUser 用户维度索引。同一用户会话允许多个连接（如多标签页），
     * 同一连接仅属于一个用户会话（重复绑定其他用户会话时先解除旧绑定）。</p>
     *
     * @param userId        用户 ID（{@link com.ghost616.platform.session.UserSession#getUser()}）
     * @param userSessionId 用户会话 ID（{@link com.ghost616.platform.session.UserSession#getSessionId()}）
     * @param session       WebSocket 连接
     * @return 绑定结果，userId/userSessionId 任一无效或连接为 null 时绑定失败
     */
    public BindResult bind(Long userId, String userSessionId, WebSocketSession session) {
        if (session == null) {
            return BindResult.fail("连接无效");
        }
        if (userId == null) {
            return BindResult.fail("用户 ID 无效");
        }
        if (userSessionId == null || userSessionId.isBlank()) {
            return BindResult.fail("用户会话 ID 无效");
        }
        String previousUserSessionId = boundUserSessionIds.get(session);
        if (previousUserSessionId != null && !previousUserSessionId.equals(userSessionId)) {
            removeConnection(previousUserSessionId, session);
        }
        connections.computeIfAbsent(userSessionId, k -> new CopyOnWriteArraySet<>()).add(session);
        boundUserSessionIds.put(session, userSessionId);
        userSessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(userSessionId);
        log.debug("WebSocket 连接绑定用户: session={}, userId={}, userSessionId={}",
                session.getId(), userId, userSessionId);
        return BindResult.ok();
    }

    /**
     * 解绑连接与用户会话，同步清理三个索引。
     *
     * @param userId        用户 ID
     * @param userSessionId 用户会话 ID
     * @param session       WebSocket 连接
     */
    public void unbind(Long userId, String userSessionId, WebSocketSession session) {
        if (session == null || userSessionId == null || userSessionId.isBlank()) {
            return;
        }
        removeConnection(userSessionId, session);
        boundUserSessionIds.remove(session, userSessionId);
        if (!connections.containsKey(userSessionId)) {
            if (userId != null) {
                userSessionsByUser.computeIfPresent(userId, (k, sessionIds) -> {
                    sessionIds.remove(userSessionId);
                    return sessionIds.isEmpty() ? null : sessionIds;
                });
            } else {
                removeIdleUserSession(userSessionId);
            }
        }
        log.debug("WebSocket 连接解绑用户: session={}, userId={}, userSessionId={}",
                session.getId(), userId, userSessionId);
    }

    /**
     * 获取指定用户会话的全部连接快照（精准获取）。
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
     * 获取指定用户的全部用户会话的所有连接快照（广播获取）。
     *
     * @param userId 用户 ID
     * @return 连接列表，用户无绑定时返回空列表
     */
    public List<WebSocketSession> getSessionsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Set<String> userSessionIds = userSessionsByUser.get(userId);
        if (userSessionIds == null || userSessionIds.isEmpty()) {
            return List.of();
        }
        List<WebSocketSession> result = new ArrayList<>();
        for (String userSessionId : userSessionIds) {
            Set<WebSocketSession> sessionConnections = connections.get(userSessionId);
            if (sessionConnections != null) {
                result.addAll(sessionConnections);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 连接关闭时清理该连接的全部注册（三个索引同步清理）。
     *
     * @param session 已关闭的连接
     */
    public void removeAll(WebSocketSession session) {
        String userSessionId = boundUserSessionIds.remove(session);
        if (userSessionId == null) {
            return;
        }
        removeConnection(userSessionId, session);
        log.debug("WebSocket 连接关闭, 清理用户会话绑定: session={}, userSessionId={}",
                session.getId(), userSessionId);
    }

    /**
     * 从主索引移除连接，若该用户会话已无任何连接则清理 userId 维度索引。
     */
    private void removeConnection(String userSessionId, WebSocketSession session) {
        Set<WebSocketSession> sessionConnections = connections.get(userSessionId);
        if (sessionConnections != null) {
            sessionConnections.remove(session);
            if (sessionConnections.isEmpty()) {
                connections.remove(userSessionId, sessionConnections);
            }
        }
        if (!connections.containsKey(userSessionId)) {
            removeIdleUserSession(userSessionId);
        }
    }

    /**
     * 从 userId 维度索引移除已无活跃连接的用户会话（遍历兜底，userId 未知时使用）。
     */
    private void removeIdleUserSession(String userSessionId) {
        userSessionsByUser.forEach((userId, sessionIds) -> {
            if (sessionIds.remove(userSessionId) && sessionIds.isEmpty()) {
                userSessionsByUser.remove(userId, sessionIds);
            }
        });
    }
}
