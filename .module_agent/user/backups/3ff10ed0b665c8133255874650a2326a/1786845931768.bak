package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.user.LoginRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserSessionManager userSessionManager;

    /**
     * 登录：校验登录名与密码，成功后创建用户会话并写入 HttpOnly Cookie。
     *
     * @param request  登录请求
     * @param response HTTP 响应
     * @return 登录用户信息
     */
    @PostMapping("/login")
    public ApiResponse<UserDTO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userService.login(request);
        UserSession session = userSessionManager.createSession(user);
        ResponseCookie cookie = ResponseCookie.from(UserSessionManager.SESSION_COOKIE_NAME, session.getSessionId())
                .httpOnly(true)
                .path("/")
                .maxAge(UserSessionManager.SESSION_MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success(UserDTO.of(user));
    }
}
