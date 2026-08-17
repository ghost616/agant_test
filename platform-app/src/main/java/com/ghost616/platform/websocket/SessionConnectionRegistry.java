package com.ghost616.platform.websocket;

import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 会话连接注册中心。
 *
 * <p>维护「会话 ID → WebSocket 连接集合」的映射（同一会话允许多个连接，如多标签页），
 * 并维护「连接 → 已绑定会话 ID 集合」的反向索引，用于连接关闭时清理注册。
 * 绑定会话时校验会话存在性（查 {@link SessionMapper}）与属主
 * （会话必须属于当前登录用户，对齐数据用户隔离约定）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionConnectionRegistry {

    /** 会话 ID → 连接集合。 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> connections = new ConcurrentHashMap<>();

    /** 连接 → 已绑定会话 ID 集合（反向索引，连接关闭时清理）。 */
    private final ConcurrentHashMap<WebSocketSession, CopyOnWriteArraySet<String>> boundSessionIds = new ConcurrentHashMap<>();

    private final SessionMapper sessionMapper;

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
     * 绑定连接与会话。
     *
     * @param userId    当前登录用户 ID
     * @param sessionId 要绑定的会话 ID
     * @param session   WebSocket 连接
     * @return 绑定结果，会话不存在或非本人会话时绑定失败
     */
    public BindResult bind(Long userId, String sessionId, WebSocketSession session) {
        if (session == null) {
            return BindResult.fail("连接无效");
        }
        Long sessionIdLong;
        try {
            sessionIdLong = IdConverter.parse(sessionId);
        } catch (IllegalArgumentException e) {
            return BindResult.fail("会话 ID 无效");
        }
        if (sessionIdLong == null) {
            return BindResult.fail("会话 ID 无效");
        }
        Session dbSession = sessionMapper.selectById(sessionIdLong);
        if (dbSession == null) {
            return BindResult.fail("会话不存在");
        }
        if (!Objects.equals(dbSession.getUserId(), userId)) {
            return BindResult.fail("无权绑定该会话");
        }
        connections.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(session);
        boundSessionIds.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        return BindResult.ok();
    }

    /**
     * 解绑连接与会话。
     *
     * @param sessionId 要解绑的会话 ID
     * @param session   WebSocket 连接
     */
    public void unbind(String sessionId, WebSocketSession session) {
        Set<WebSocketSession> sessionConnections = connections.get(sessionId);
        if (sessionConnections != null) {
            sessionConnections.remove(session);
            if (sessionConnections.isEmpty()) {
                connections.remove(sessionId, sessionConnections);
            }
        }
        Set<String> boundSessions = boundSessionIds.get(session);
        if (boundSessions != null) {
            boundSessions.remove(sessionId);
            if (boundSessions.isEmpty()) {
                boundSessionIds.remove(session, boundSessions);
            }
        }
    }

    /**
     * 获取指定会话的全部连接快照。
     *
     * @param sessionId 会话 ID
     * @return 连接列表，无绑定时返回空列表
     */
    public List<WebSocketSession> getSessions(String sessionId) {
        Set<WebSocketSession> sessionConnections = connections.get(sessionId);
        if (sessionConnections == null || sessionConnections.isEmpty()) {
            return List.of();
        }
        return List.copyOf(sessionConnections);
    }

    /**
     * 连接关闭时清理该连接注册的所有会话绑定。
     *
     * @param session 已关闭的连接
     */
    public void removeAll(WebSocketSession session) {
        Set<String> boundSessions = boundSessionIds.remove(session);
        if (boundSessions == null) {
            return;
        }
        for (String sessionId : boundSessions) {
            Set<WebSocketSession> sessionConnections = connections.get(sessionId);
            if (sessionConnections != null) {
                sessionConnections.remove(session);
                if (sessionConnections.isEmpty()) {
                    connections.remove(sessionId, sessionConnections);
                }
            }
        }
        log.debug("WebSocket 连接关闭, 清理 {} 个会话绑定", boundSessions.size());
    }
}