package com.ghost616.platform.session;

/**
 * 线程上下文，通过 ThreadLocal 保存当前请求线程的用户会话。
 *
 * <p>由鉴权拦截器在请求进入时写入、请求结束时清理，
 * 业务代码可通过 {@link #get()} 获取当前登录用户会话。</p>
 */
public final class UserContext {

    private static final ThreadLocal<UserSession> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前线程的用户会话。
     *
     * @param session 用户会话
     */
    public static void set(UserSession session) {
        HOLDER.set(session);
    }

    /**
     * 获取当前线程的用户会话。
     *
     * @return 用户会话，无会话时返回 null
     */
    public static UserSession get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程的用户会话，防止线程复用导致会话串号。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
