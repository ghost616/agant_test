package com.ghost616.platform.config;

import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器。
 *
 * <p>在请求进入控制器前，从 Cookie 中读取会话 ID（SESSION_ID），
 * 调用 {@link UserSessionManager#getSession(String)} 查询会话
 * （该方法会同步刷新会话最后访问时间），并将用户会话写入
 * {@link UserContext} 线程上下文；请求结束时清理线程上下文，
 * 防止线程复用导致会话串号。</p>
 *
 * <p>本拦截器只负责写入登录上下文，不阻断任何请求；
 * 是否登录、是否具有权限由 {@link AuthAspect} 统一校验，
 * 登录接口（POST /api/auth/login）在 {@link WebConfig} 中放行。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserSessionManager userSessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sessionId = resolveSessionId(request);
        UserSession session = userSessionManager.getSession(sessionId);
        if (session != null) {
            UserContext.set(session);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 从 Cookie 中解析会话 ID。
     *
     * @param request HTTP 请求
     * @return 会话 ID，Cookie 缺失或未找到会话 Cookie 时返回 null
     */
    private String resolveSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (UserSessionManager.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}