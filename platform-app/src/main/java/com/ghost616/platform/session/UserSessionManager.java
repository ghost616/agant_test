package com.ghost616.platform.session;

import com.ghost616.platform.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户会话管理器，基于内存 ConcurrentHashMap 保存用户会话。
 *
 * <p>支持按会话 ID 创建、查询、删除、刷新会话；会话空闲超过 2 小时过期，
 * 由定时任务周期清理过期会话。会话 ID 通过 HttpOnly Cookie 下发给客户端。</p>
 */
@Slf4j
@Component
public class UserSessionManager {

    /** 会话 Cookie 名称。 */
    public static final String SESSION_COOKIE_NAME = "SESSION_ID";

    /** 会话最大空闲时间（秒），2 小时。 */
    public static final long SESSION_MAX_AGE_SECONDS = 2 * 60 * 60;

    /** 会话空闲过期时间（毫秒）。 */
    private static final long IDLE_TIMEOUT_MILLIS = SESSION_MAX_AGE_SECONDS * 1000;

    /** 定时清理间隔（毫秒），1 分钟。 */
    private static final long CLEANUP_INTERVAL_MILLIS = 60 * 1000;

    private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    /**
     * 为用户创建新会话。
     *
     * @param user 登录用户
     * @return 创建的用户会话
     */
    public UserSession createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(sessionId, user, System.currentTimeMillis());
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * 按会话 ID 查询会话，命中时刷新最后访问时间。
     *
     * @param sessionId 会话 ID
     * @return 用户会话，不存在时返回 null
     */
    public UserSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        UserSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessTime(System.currentTimeMillis());
        }
        return session;
    }

    /**
     * 刷新指定会话的最后访问时间。
     *
     * @param sessionId 会话 ID
     */
    public void refresh(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        UserSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessTime(System.currentTimeMillis());
        }
    }

    /**
     * 删除指定会话（注销或强制下线）。
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessions.remove(sessionId);
    }

    /**
     * 定时清理空闲超时的会话。
     */
    @Scheduled(initialDelay = CLEANUP_INTERVAL_MILLIS, fixedDelay = CLEANUP_INTERVAL_MILLIS)
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (UserSession session : sessions.values()) {
            if (now - session.getLastAccessTime() > IDLE_TIMEOUT_MILLIS) {
                sessions.remove(session.getSessionId(), session);
                removed++;
            }
        }
        if (removed > 0) {
            log.info("用户会话清理: 移除 {} 个过期会话", removed);
        }
    }
}
