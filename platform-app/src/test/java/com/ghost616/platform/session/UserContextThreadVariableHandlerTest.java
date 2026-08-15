package com.ghost616.platform.session;

import com.ghost616.agentbase.core.ThreadVariableWrapper;
import com.ghost616.platform.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserContextThreadVariableHandler 单元测试。
 *
 * <p>验证 wrap() 捕获 UserContext 会话快照、apply() 在目标线程恢复/清理上下文的语义，
 * 以及异步线程通过 wrapper 传播用户上下文的能力。</p>
 */
class UserContextThreadVariableHandlerTest {

    private final UserContextThreadVariableHandler handler = new UserContextThreadVariableHandler();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private UserSession newSession(Long userId) {
        User user = new User();
        user.setId(userId);
        return new UserSession("ctx-test", user, System.currentTimeMillis());
    }

    @Test
    void wrap_捕获当前用户会话_apply恢复() {
        UserSession session = newSession(42L);
        UserContext.set(session);

        ThreadVariableWrapper wrapper = handler.wrap();
        assertNotNull(wrapper);

        UserContext.clear();
        wrapper.apply();

        assertSame(session, UserContext.get(), "apply() 应恢复 wrap() 时捕获的会话");
        assertEquals(42L, UserContext.get().getUser().getId());
    }

    @Test
    void wrap_无用户上下文_apply清空目标线程残留上下文() {
        UserContext.clear();
        ThreadVariableWrapper wrapper = handler.wrap();
        assertNotNull(wrapper);

        // 目标线程已有残留上下文（线程复用场景）
        UserContext.set(newSession(7L));
        wrapper.apply();

        assertNull(UserContext.get(), "捕获为 null 时 apply() 应清空当前线程上下文");
    }

    @Test
    void apply_清空主线程后_异步线程通过wrapper恢复上下文() throws Exception {
        UserSession session = newSession(99L);
        UserContext.set(session);
        ThreadVariableWrapper wrapper = handler.wrap();
        UserContext.clear();

        Thread t = new Thread(() -> {
            wrapper.apply();
            try {
                assertSame(session, UserContext.get(), "异步线程应能读到捕获的会话");
            } finally {
                UserContext.clear();
            }
        });
        t.start();
        t.join(5000);

        assertFalse(t.isAlive(), "异步线程应正常结束");
        assertNull(UserContext.get(), "主线程上下文不应被异步线程污染");
    }

    @Test
    void 多次apply_可重复恢复() {
        UserSession session = newSession(5L);
        UserContext.set(session);
        ThreadVariableWrapper wrapper = handler.wrap();
        UserContext.clear();

        wrapper.apply();
        assertSame(session, UserContext.get());
        UserContext.clear();
        wrapper.apply();
        assertSame(session, UserContext.get());
    }
}