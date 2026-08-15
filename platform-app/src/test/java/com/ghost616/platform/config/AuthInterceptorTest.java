package com.ghost616.platform.config;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthInterceptor 单元测试（不使用 Mockito，沙箱环境禁子进程）。
 *
 * <p>覆盖：有效会话写入 UserContext 且不阻断、无 Cookie / 无会话 Cookie /
 * 会话不存在 / 空会话值时不写上下文且不阻断、afterCompletion 清理上下文。</p>
 */
class AuthInterceptorTest {

    /** 可编程 UserSessionManager 桩：仅覆盖 getSession。 */
    static class StubSessionManager extends UserSessionManager {
        UserSession result;
        String lastSessionId;

        @Override
        public UserSession getSession(String sessionId) {
            lastSessionId = sessionId;
            return result;
        }
    }

    private StubSessionManager sessionManager;
    private AuthInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserSession adminSession;

    @BeforeEach
    void setUp() {
        sessionManager = new StubSessionManager();
        interceptor = new AuthInterceptor(sessionManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        User user = new User();
        user.setLoginName("admin");
        user.setUserType(UserService.USER_TYPE_ADMIN);
        adminSession = new UserSession("sid-1", user, 1000L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void 会话存在时写入上下文并返回true() {
        request.setCookies(new Cookie(UserSessionManager.SESSION_COOKIE_NAME, "sid-1"));
        sessionManager.result = adminSession;
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertSame(adminSession, UserContext.get());
        assertTrue("sid-1".equals(sessionManager.lastSessionId));
    }

    @Test
    void 无Cookie时不写上下文返回true() {
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertNull(UserContext.get());
        assertNull(sessionManager.lastSessionId);
    }

    @Test
    void 无会话Cookie时不写上下文返回true() {
        request.setCookies(new Cookie("OTHER", "x"));
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertNull(UserContext.get());
        assertNull(sessionManager.lastSessionId);
    }

    @Test
    void 会话不存在时不写上下文返回true() {
        request.setCookies(new Cookie(UserSessionManager.SESSION_COOKIE_NAME, "bad-sid"));
        sessionManager.result = null;
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertNull(UserContext.get());
        assertTrue("bad-sid".equals(sessionManager.lastSessionId));
    }

    @Test
    void 会话Cookie值为空时不写上下文返回true() {
        request.setCookies(new Cookie(UserSessionManager.SESSION_COOKIE_NAME, ""));
        sessionManager.result = null;
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertNull(UserContext.get());
    }

    @Test
    void afterCompletion清理上下文() {
        UserContext.set(adminSession);
        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserContext.get());
    }
}