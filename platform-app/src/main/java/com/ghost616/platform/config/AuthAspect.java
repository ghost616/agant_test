package com.ghost616.platform.config;

import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 控制器鉴权切面。
 *
 * <p>对 {@code com.ghost616.platform.controller} 包下所有控制器方法统一鉴权：
 * 当前线程无用户会话（未登录）时抛出 {@link ErrorCode#USER_NOT_LOGIN}；
 * 用户管理接口（/api/users/**）额外要求管理员用户类型，否则抛出
 * {@link ErrorCode#USER_FORBIDDEN}。登录接口（POST /api/auth/login）
 * 与 CORS 预检请求（OPTIONS）直接放行。</p>
 */
@Aspect
@Component
public class AuthAspect {

    /** 登录接口路径，无需登录即可访问。 */
    private static final String LOGIN_PATH = "/api/auth/login";

    /** 用户管理接口路径前缀，要求管理员权限。 */
    private static final String USERS_PATH_PREFIX = "/api/users";

    /**
     * 控制器方法统一鉴权。
     *
     * @param joinPoint 连接点
     * @return 控制器方法返回值
     * @throws Throwable 控制器方法异常或鉴权异常
     */
    @Around("execution(* com.ghost616.platform.controller..*(..))")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || "OPTIONS".equalsIgnoreCase(request.getMethod()) || isLoginPath(request)) {
            return joinPoint.proceed();
        }
        UserSession session = UserContext.get();
        if (session == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        if (request.getRequestURI().startsWith(USERS_PATH_PREFIX) && !isAdmin(session)) {
            throw new BusinessException(ErrorCode.USER_FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    /**
     * 判断当前请求是否为登录接口。
     *
     * @param request HTTP 请求
     * @return 是登录接口返回 true
     */
    private boolean isLoginPath(HttpServletRequest request) {
        return LOGIN_PATH.equals(request.getRequestURI());
    }

    /**
     * 判断会话用户是否为管理员。
     *
     * @param session 用户会话
     * @return 管理员返回 true
     */
    private boolean isAdmin(UserSession session) {
        Integer userType = session.getUser().getUserType();
        return userType != null && userType == UserService.USER_TYPE_ADMIN;
    }

    /**
     * 获取当前请求。
     *
     * @return HTTP 请求，无请求上下文时返回 null
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}