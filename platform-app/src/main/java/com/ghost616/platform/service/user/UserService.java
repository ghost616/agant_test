package com.ghost616.platform.service.user;

import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.user.LoginRequest;
import com.ghost616.platform.dto.user.UserCreateRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserSelfUpdateRequest;
import com.ghost616.platform.dto.user.UserUpdateRequest;
import com.ghost616.platform.entity.User;

/**
 * 用户服务：登录校验、用户增改查。
 */
public interface UserService {

    /** 普通用户类型。 */
    int USER_TYPE_NORMAL = 1;

    /** 管理员用户类型。 */
    int USER_TYPE_ADMIN = 2;

    /**
     * 登录校验：校验登录名与密码，并校验登录开关。
     *
     * @param request 登录请求
     * @return 登录成功的用户实体
     */
    User login(LoginRequest request);

    /**
     * 分页查询用户列表。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<UserDTO> pageUsers(int page, int size);

    /**
     * 添加用户。
     *
     * @param request 添加用户请求
     * @return 新增用户信息
     */
    UserDTO createUser(UserCreateRequest request);

    /**
     * 修改用户。
     *
     * @param id      用户 ID
     * @param request 修改用户请求
     * @return 修改后的用户信息
     */
    UserDTO updateUser(Long id, UserUpdateRequest request);

    /**
     * 当前登录用户自助修改自己的显示名与密码（enabled 不可自助修改）。
     *
     * @param userId  当前登录用户 ID
     * @param request 自助修改请求，字段为空表示不修改
     * @return 修改后的用户信息
     */
    UserDTO updateSelf(Long userId, UserSelfUpdateRequest request);
}
