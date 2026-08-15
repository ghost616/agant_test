package com.ghost616.platform.session;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;

/**
 * 用户上下文工具类，为其它模块获取当前登录用户 ID 的统一入口。
 *
 * <p>内部通过 {@link UserContext} 读取当前请求线程的用户会话，
 * 需要强制登录的场景使用 {@link #requireUserId()}，
 * 允许匿名场景使用 {@link #currentUserIdOrNull()}。</p>
 */
public final class UserContextUtil {

    private UserContextUtil() {
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出异常。
     *
     * @return 当前登录用户 ID
     * @throws BusinessException 会话为空或用户为空时抛出 {@link ErrorCode#USER_NOT_LOGIN}
     */
    public static Long requireUserId() {
        User user = currentUserOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return user.getId();
    }

    /**
     * 获取当前登录用户 ID，未登录时返回 null（不抛异常）。
     *
     * @return 当前登录用户 ID，会话为空或用户为空时返回 null
     */
    public static Long currentUserIdOrNull() {
        User user = currentUserOrNull();
        return user == null ? null : user.getId();
    }

    /**
     * 获取当前登录用户，会话为空或用户为空时返回 null。
     *
     * @return 当前登录用户，未登录时返回 null
     */
    private static User currentUserOrNull() {
        UserSession session = UserContext.get();
        return session == null ? null : session.getUser();
    }
}