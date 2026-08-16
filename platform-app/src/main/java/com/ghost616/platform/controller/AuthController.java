package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.user.LoginRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserSelfUpdateRequest;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserContextUtil;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、退出登录、当前登录用户信息自助修改。
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

    /**
     * 退出登录：清除服务端用户会话并删除 HttpOnly 会话 Cookie（立即过期）。
     *
     * @param response HTTP 响应
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        UserSession session = UserContext.get();
        if (session != null) {
            userSessionManager.removeSession(session.getSessionId());
        }
        ResponseCookie cookie = ResponseCookie.from(UserSessionManager.SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success(null);
    }

    /**
     * 当前登录用户自助修改自己的显示名与密码（enabled 不可自助修改）。
     *
     * @param request 自助修改请求，字段为空表示不修改
     * @return 修改后的用户信息
     */
    @PutMapping("/me")
    public ApiResponse<UserDTO> updateSelf(@Valid @RequestBody UserSelfUpdateRequest request) {
        Long userId = UserContextUtil.requireUserId();
        return ApiResponse.success(userService.updateSelf(userId, request));
    }
}
