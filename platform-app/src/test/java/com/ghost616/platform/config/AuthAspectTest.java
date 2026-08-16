package com.ghost616.platform.config;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthAspect 单元测试（不使用 Mockito，沙箱环境禁子进程）。
 *
 * <p>覆盖：无请求上下文/OPTIONS 预检/登录接口放行、未登录抛 USER_NOT_LOGIN、
 * 普通用户访问 /api/users 抛 USER_FORBIDDEN、管理员放行、普通用户访问其他接口放行。</p>
 */
class AuthAspectTest {

    private final AuthAspect aspect = new AuthAspect();

    /** 记录是否 proceed 的 JoinPoint 桩。 */
    static class StubJoinPoint implements ProceedingJoinPoint {
        boolean proceeded;

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {
            // no-op：单元测试不经过 aspectj 编织
        }

        @Override
        public Object proceed() {
            proceeded = true;
            return "ok";
        }

        @Override
        public Object proceed(Object[] args) {
            return "ok";
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Signature getSignature() {
            return null;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String getKind() {
            return METHOD_EXECUTION;
        }

        @Override
        public String toShortString() {
            return "stub";
        }

        @Override
        public String toLongString() {
            return "stub";
        }

        @Override
        public String toString() {
            return "stub";
        }
    }

    private MockHttpServletRequest mockRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        return request;
    }

    private void bindRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private UserSession sessionOfType(Integer userType) {
        User user = new User();
        user.setLoginName("u");
        user.setUserType(userType);
        return new UserSession("sid", user, 1000L);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContext.clear();
    }

    @Test
    void 无请求上下文时放行() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }

    @Test
    void OPTIONS预检请求放行() throws Throwable {
        bindRequest(mockRequest("OPTIONS", "/api/users"));
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }

    @Test
    void 登录接口放行() throws Throwable {
        bindRequest(mockRequest("POST", "/api/auth/login"));
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }

    @Test
    void 未登录访问普通接口抛USER_NOT_LOGIN() throws Throwable {
        bindRequest(mockRequest("GET", "/api/models"));
        UserContext.clear();
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        assertEquals("请登录", ex.getMessage());
        assertFalse(jp.proceeded);
    }

    @Test
    void 未登录访问authMe接口抛USER_NOT_LOGIN() throws Throwable {
        bindRequest(mockRequest("PUT", "/api/auth/me"));
        UserContext.clear();
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        assertFalse(jp.proceeded);
    }

    @Test
    void 未登录访问logout接口抛USER_NOT_LOGIN() throws Throwable {
        bindRequest(mockRequest("POST", "/api/auth/logout"));
        UserContext.clear();
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        assertFalse(jp.proceeded);
    }

    @Test
    void 普通用户访问authMe接口放行() throws Throwable {
        bindRequest(mockRequest("PUT", "/api/auth/me"));
        UserContext.set(sessionOfType(UserService.USER_TYPE_NORMAL));
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }

    @Test
    void 未登录访问users接口抛USER_NOT_LOGIN而非FORBIDDEN() throws Throwable {
        bindRequest(mockRequest("GET", "/api/users"));
        UserContext.clear();
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        assertFalse(jp.proceeded);
    }

    @Test
    void 普通用户访问users接口抛USER_FORBIDDEN() throws Throwable {
        bindRequest(mockRequest("GET", "/api/users"));
        UserContext.set(sessionOfType(UserService.USER_TYPE_NORMAL));
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_FORBIDDEN, ex.getErrorCode());
        assertEquals("无权限", ex.getMessage());
        assertFalse(jp.proceeded);
    }

    @Test
    void 普通用户访问users子路径抛USER_FORBIDDEN() throws Throwable {
        bindRequest(mockRequest("GET", "/api/users/123"));
        UserContext.set(sessionOfType(UserService.USER_TYPE_NORMAL));
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_FORBIDDEN, ex.getErrorCode());
        assertFalse(jp.proceeded);
    }

    @Test
    void 用户类型为null访问users抛USER_FORBIDDEN() throws Throwable {
        bindRequest(mockRequest("GET", "/api/users"));
        UserContext.set(sessionOfType(null));
        StubJoinPoint jp = new StubJoinPoint();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkAuth(jp));
        assertEquals(ErrorCode.USER_FORBIDDEN, ex.getErrorCode());
        assertFalse(jp.proceeded);
    }

    @Test
    void 管理员访问users接口放行() throws Throwable {
        bindRequest(mockRequest("GET", "/api/users"));
        UserContext.set(sessionOfType(UserService.USER_TYPE_ADMIN));
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }

    @Test
    void 普通用户访问非users接口放行() throws Throwable {
        bindRequest(mockRequest("GET", "/api/models"));
        UserContext.set(sessionOfType(UserService.USER_TYPE_NORMAL));
        StubJoinPoint jp = new StubJoinPoint();
        assertEquals("ok", aspect.checkAuth(jp));
        assertTrue(jp.proceeded);
    }
}