package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.user.LoginRequest;
import com.ghost616.platform.dto.user.UserCreateRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserSelfUpdateRequest;
import com.ghost616.platform.dto.user.UserUpdateRequest;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.service.user.UserService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthController 单元测试（不使用 Mockito，手写桩）。
 *
 * <p>覆盖：PUT /api/auth/me 登录用户自助修改（返回不含密码的 UserDTO）、
 * 未登录抛 USER_NOT_LOGIN；POST /api/auth/logout 清除服务端会话并下发
 * maxAge=0 的 HttpOnly Cookie。</p>
 */
class AuthControllerTest {

    /** 可编程 UserService 桩。 */
    static class StubUserService implements UserService {
        Long lastUpdateSelfUserId;
        UserSelfUpdateRequest lastUpdateSelfRequest;
        UserDTO updateSelfResult;

        @Override
        public UserDTO updateSelf(Long userId, UserSelfUpdateRequest request) {
            lastUpdateSelfUserId = userId;
            lastUpdateSelfRequest = request;
            return updateSelfResult;
        }

        @Override
        public User login(LoginRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<UserDTO> pageUsers(int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDTO createUser(UserCreateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDTO updateUser(Long id, UserUpdateRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    /** 记录 removeSession 调用的会话管理器桩。 */
    static class StubSessionManager extends UserSessionManager {
        String removedSessionId;

        @Override
        public void removeSession(String sessionId) {
            removedSessionId = sessionId;
        }
    }

    private StubUserService userService;
    private StubSessionManager sessionManager;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userService = new StubUserService();
        sessionManager = new StubSessionManager();
        controller = new AuthController(userService, sessionManager);
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserSession sessionOf(Long userId, int userType) {
        User user = new User();
        user.setId(userId);
        user.setLoginName("normal-user");
        user.setUserType(userType);
        user.setEnabled(1);
        return new UserSession("sid-" + userId, user, 1000L);
    }

    // ================= PUT /api/auth/me =================

    @Test
    void updateSelf_普通用户登录可修改自己() {
        UserContext.set(sessionOf(42L, UserService.USER_TYPE_NORMAL));
        UserDTO expected = UserDTO.builder().id(42L).loginName("normal-user")
                .displayName("新昵称").userType(UserService.USER_TYPE_NORMAL).enabled(1).build();
        userService.updateSelfResult = expected;

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().displayName("新昵称").build();
        ApiResponse<UserDTO> response = controller.updateSelf(request);

        assertTrue(response.isSuccess());
        assertEquals(42L, userService.lastUpdateSelfUserId, "应使用当前登录用户 ID 调用 updateSelf");
        assertNotNull(userService.lastUpdateSelfRequest);
        assertEquals("新昵称", response.getData().getDisplayName());
    }

    @Test
    void updateSelf_未登录抛USER_NOT_LOGIN() {
        UserContext.clear();
        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().displayName("x").build();
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.updateSelf(request));
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        assertNull(userService.lastUpdateSelfUserId, "未登录不应调用服务层");
    }

    @Test
    void updateSelf_返回的UserDTO不含密码字段() throws Exception {
        UserContext.set(sessionOf(7L, UserService.USER_TYPE_NORMAL));
        UserDTO expected = UserDTO.builder().id(7L).loginName("u").userType(1).enabled(1).build();
        userService.updateSelfResult = expected;

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().build();
        ApiResponse<UserDTO> response = controller.updateSelf(request);

        assertNotNull(response.getData());
        for (Field f : response.getData().getClass().getDeclaredFields()) {
            assertFalse("password".equals(f.getName()), "UserDTO 不得包含密码字段");
        }
    }

    @Test
    void updateSelf_请求体不含enabled字段_不可自助修改登录开关() throws Exception {
        // 编译期 UserSelfUpdateRequest 已无 enabled 字段；此处反射兜底验证
        for (Field f : UserSelfUpdateRequest.class.getDeclaredFields()) {
            assertFalse("enabled".equals(f.getName()), "UserSelfUpdateRequest 不得包含 enabled 字段");
        }
    }

    // ================= POST /api/auth/logout =================

    @Test
    void logout_清除服务端会话并下发过期Cookie() {
        UserContext.set(sessionOf(1L, UserService.USER_TYPE_ADMIN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiResponse<Void> apiResponse = controller.logout(response);

        assertTrue(apiResponse.isSuccess(), "退出登录应返回成功");
        assertEquals("sid-1", sessionManager.removedSessionId, "应清除当前会话");
        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie, "应下发 Set-Cookie 使会话 Cookie 过期");
        assertTrue(setCookie.contains("SESSION_ID"), "Cookie 名应为 SESSION_ID");
        assertTrue(setCookie.contains("Max-Age=0"), "Cookie 应立即过期（maxAge=0）");
        assertTrue(setCookie.contains("HttpOnly"), "会话 Cookie 应保持 HttpOnly");
    }

    @Test
    void logout_无会话时仅清理Cookie不抛异常() {
        // 控制器层设计：无会话仍返回成功并清理 Cookie；未登录拦截由 AuthAspect 统一负责（另有测试覆盖）
        UserContext.clear();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiResponse<Void> apiResponse = controller.logout(response);

        assertTrue(apiResponse.isSuccess());
        assertNull(sessionManager.removedSessionId, "无会话不应调用 removeSession");
        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Max-Age=0"));
    }

    @Test
    void logout_返回SetCookie使Cookie过期且值为空() {
        UserContext.set(sessionOf(2L, UserService.USER_TYPE_NORMAL));
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.logout(response);
        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.contains("SESSION_ID=;"), "过期 Cookie 值应为空");
        assertTrue(setCookie.contains("Path=/"), "Cookie 路径应与登录一致");
    }
}