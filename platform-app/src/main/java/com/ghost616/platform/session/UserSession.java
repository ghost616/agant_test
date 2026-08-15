package com.ghost616.platform.session;

import com.ghost616.platform.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户会话对象，表示一次登录建立的用户会话。
 *
 * <p>包含会话 ID、登录用户信息以及最后访问时间（epoch 毫秒）。
 * 最后访问时间由 {@link UserSessionManager} 在会话被访问时刷新，
 * 用于空闲超时过期判断。</p>
 */
@Data
@AllArgsConstructor
public class UserSession {

    /** 会话 ID（全局唯一）。 */
    private final String sessionId;

    /** 会话对应的登录用户信息。 */
    private final User user;

    /** 最后访问时间（epoch 毫秒），多线程可见。 */
    private volatile long lastAccessTime;
}
