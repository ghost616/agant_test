package com.ghost616.platform.session;

import com.ghost616.agentbase.core.ThreadVariableHandler;
import com.ghost616.agentbase.core.ThreadVariableWrapper;
import org.springframework.stereotype.Component;

/**
 * 用户上下文线程变量处理器。
 *
 * <p>实现 agent-base 的 {@link ThreadVariableHandler}，在提交异步任务（如工具执行的
 * {@code CompletableFuture.supplyAsync}）前捕获 {@link UserContext} 线程上下文，
 * 异步线程开始执行时通过 {@link UserContextThreadVariableWrapper#apply()} 恢复，
 * 保证异步场景下 {@link UserContext#get()} 仍可取到当前登录用户。</p>
 */
@Component
public class UserContextThreadVariableHandler implements ThreadVariableHandler {

    @Override
    public ThreadVariableWrapper wrap() {
        UserSession session = UserContext.get();
        return new UserContextThreadVariableWrapper(session);
    }

    /**
     * 用户上下文线程变量包装器，承载捕获的用户会话快照。
     */
    private static class UserContextThreadVariableWrapper implements ThreadVariableWrapper {

        private final UserSession session;

        UserContextThreadVariableWrapper(UserSession session) {
            this.session = session;
        }

        @Override
        public void apply() {
            if (session != null) {
                UserContext.set(session);
            } else {
                UserContext.clear();
            }
        }
    }
}