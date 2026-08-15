package com.ghost616.platform.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.user.LoginRequest;
import com.ghost616.platform.dto.user.UserCreateRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserUpdateRequest;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.UserMapper;
import com.ghost616.platform.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 允许登录的 enabled 值。 */
    private static final int ENABLED_YES = 1;

    private final UserMapper userMapper;

    @Override
    public User login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getLoginName, request.getLoginName()));
        if (user == null || !matchesPassword(request.getPassword(), user)) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }
        if (user.getEnabled() == null || user.getEnabled() != ENABLED_YES) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    @Override
    public PageResult<UserDTO> pageUsers(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        Page<User> pager = new Page<>(safePage, safeSize);
        userMapper.selectPage(pager, new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<UserDTO> list = pager.getRecords().stream().map(UserDTO::of).toList();
        return PageResult.of(pager, list);
    }

    @Override
    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        checkLoginNameDuplicate(request.getLoginName(), null);

        User user = new User();
        user.setId(IdWorker.getId());
        user.setLoginName(request.getLoginName());
        user.setDisplayName(request.getDisplayName());
        user.setUserType(validateUserType(request.getUserType()));
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : ENABLED_YES);
        LocalDateTime createTime = LocalDateTime.now().withNano(0);
        user.setCreateTime(createTime);
        user.setPassword(encryptPassword(request.getPassword(), user.getId(), createTime));

        userMapper.insert(user);
        return UserDTO.of(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (StringUtils.isNotBlank(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getUserType() != null) {
            user.setUserType(validateUserType(request.getUserType()));
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            if (user.getCreateTime() == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "用户缺少创建时间，无法重置密码");
            }
            user.setPassword(encryptPassword(request.getPassword(), user.getId(), user.getCreateTime()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(validateEnabled(request.getEnabled()));
        }
        userMapper.updateById(user);
        return UserDTO.of(user);
    }

    private void checkLoginNameDuplicate(String loginName, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getLoginName, loginName);
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private int validateUserType(Integer userType) {
        int type = userType != null ? userType : USER_TYPE_NORMAL;
        if (type != USER_TYPE_NORMAL && type != USER_TYPE_ADMIN) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "userType 只能为 1（普通用户）或 2（管理员）");
        }
        return type;
    }

    private int validateEnabled(Integer enabled) {
        if (enabled != 0 && enabled != 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "enabled 只能为 0 或 1");
        }
        return enabled;
    }

    private boolean matchesPassword(String plainPassword, User user) {
        if (user.getCreateTime() == null) {
            return false;
        }
        long createTimeMillis = user.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String encrypted = PasswordUtil.encrypt(plainPassword, String.valueOf(user.getId()), createTimeMillis);
        return encrypted.equals(user.getPassword());
    }

    private String encryptPassword(String plainPassword, Long userId, LocalDateTime createTime) {
        long createTimeMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return PasswordUtil.encrypt(plainPassword, String.valueOf(userId), createTimeMillis);
    }
}
