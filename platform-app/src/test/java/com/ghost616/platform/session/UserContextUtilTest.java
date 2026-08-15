package com.ghost616.platform.session;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UserContextUtil 单元测试。
 *
 * <p>验证 {@link UserContextUtil#requireUserId()} 与
 * {@link UserContextUtil#currentUserIdOrNull()} 在会话存在/为空、用户存在/为空
 * 场景下的返回值与异常行为，以及 UserContext 的线程隔离语义。</p>
 */
class UserContextUtilTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserSession newSession(Long userId) {
        User user = new User();
        user.setId(userId);
        return new UserSession("ctx-test", user, System.currentTimeMillis());
    }

    // ---------- 会话存在且用户存在 ----------

    @Test
    void requireUserId_会话存在用户存在_返回用户ID() {
        UserContext.set(newSession(123L));
        assertEquals(123L, UserContextUtil.requireUserId(), "登录态下应返回用户 ID");
    }

    @Test
    void currentUserIdOrNull_会话存在用户存在_返回用户ID() {
        UserContext.set(newSession(456L));
        assertEquals(456L, UserContextUtil.currentUserIdOrNull(), "登录态下应返回用户 ID");
    }

    // ---------- 会话为空 ----------

    @Test
    void requireUserId_会话为空_抛出BusinessException且code为USER_NOT_LOGIN() {
        UserContext.clear();
        BusinessException ex = assertThrows(BusinessException.class, UserContextUtil::requireUserId);
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode(), "错误码应为 USER_NOT_LOGIN");
        assertEquals("请登录", ex.getMessage(), "异常消息应为 USER_NOT_LOGIN 的消息");
    }

    @Test
    void requireUserId_显式setNull会话_抛出USER_NOT_LOGIN() {
        UserContext.set(null);
        BusinessException ex = assertThrows(BusinessException.class, UserContextUtil::requireUserId);
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
    }

    @Test
    void currentUserIdOrNull_会话为空_返回null() {
        UserContext.clear();
        assertNull(UserContextUtil.currentUserIdOrNull(), "无会话时应返回 null 而非抛异常");
    }

    @Test
    void currentUserIdOrNull_显式setNull会话_返回null() {
        UserContext.set(null);
        assertNull(UserContextUtil.currentUserIdOrNull());
    }

    // ---------- 会话存在但用户为空 ----------

    @Test
    void requireUserId_会话存在用户为空_抛出USER_NOT_LOGIN() {
        UserContext.set(new UserSession("ctx-test", null, System.currentTimeMillis()));
        BusinessException ex = assertThrows(BusinessException.class, UserContextUtil::requireUserId);
        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode(), "用户为空应按未登录处理");
    }

    @Test
    void currentUserIdOrNull_会话存在用户为空_返回null() {
        UserContext.set(new UserSession("ctx-test", null, System.currentTimeMillis()));
        assertNull(UserContextUtil.currentUserIdOrNull(), "用户为空时应返回 null");
    }

    // ---------- 边界值 ----------

    @Test
    void requireUserId_用户存在但ID为null_返回null() {
        // 会话与用户均存在但 id 未赋值：方法透传 user.getId()，返回 null（不抛异常）
        UserContext.set(newSession(null));
        assertNull(UserContextUtil.requireUserId());
    }

    // ---------- 线程隔离（ThreadLocal 语义） ----------

    @Test
    void requireUserId_子线程无会话_抛出USER_NOT_LOGIN且不影响主线程() throws Exception {
        UserContext.set(newSession(7L));
        final Throwable[] err = new Throwable[1];
        Thread t = new Thread(() -> {
            try {
                UserContextUtil.requireUserId();
                err[0] = new AssertionError("子线程未登录应抛出 BusinessException");
            } catch (BusinessException e) {
                if (e.getErrorCode() != ErrorCode.USER_NOT_LOGIN) {
                    err[0] = e;
                }
            }
        });
        t.start();
        t.join(5000);
        assertFalse(t.isAlive(), "子线程应正常结束");
        assertNull(err[0], "子线程应抛出 USER_NOT_LOGIN 错误码");
        assertEquals(7L, UserContextUtil.requireUserId(), "主线程上下文不应被子线程污染");
    }

    @Test
    void currentUserIdOrNull_子线程无会话_返回null且不影响主线程() throws Exception {
        UserContext.set(newSession(8L));
        final Long[] sub = new Long[1];
        Thread t = new Thread(() -> sub[0] = UserContextUtil.currentUserIdOrNull());
        t.start();
        t.join(5000);
        assertNull(sub[0], "子线程无会话应返回 null");
        assertEquals(8L, UserContextUtil.currentUserIdOrNull(), "主线程上下文不应被子线程污染");
    }
}