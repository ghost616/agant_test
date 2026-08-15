package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.user.UserCreateRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserUpdateRequest;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口，仅管理员可调用。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSessionManager userSessionManager;

    /**
     * 分页查询用户列表。
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param request HTTP 请求
     * @return 分页用户列表
     */
    @GetMapping
    public ApiResponse<PageResult<UserDTO>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.success(userService.pageUsers(page, size));
    }

    /**
     * 添加用户。
     *
     * @param createRequest 添加用户请求
     * @param request       HTTP 请求
     * @return 新增用户信息
     */
    @PostMapping
    public ApiResponse<UserDTO> create(@Valid @RequestBody UserCreateRequest createRequest,
                                       HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.success(userService.createUser(createRequest));
    }

    /**
     * 修改用户。
     *
     * @param id           用户 ID
     * @param updateRequest 修改用户请求
     * @param request      HTTP 请求
     * @return 修改后的用户信息
     */
    @PutMapping("/{id}")
    public ApiResponse<UserDTO> update(@PathVariable Long id,
                                       @Valid @RequestBody UserUpdateRequest updateRequest,
                                       HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.success(userService.updateUser(id, updateRequest));
    }

    /**
     * 校验当前请求用户是否为管理员。
     *
     * @param request HTTP 请求
     */
    private void requireAdmin(HttpServletRequest request) {
        UserSession session = resolveSession(request);
        if (session == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        User user = session.getUser();
        if (user.getUserType() == null || user.getUserType() != UserService.USER_TYPE_ADMIN) {
            throw new BusinessException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private UserSession resolveSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (UserSessionManager.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return userSessionManager.getSession(cookie.getValue());
            }
        }
        return null;
    }
}
