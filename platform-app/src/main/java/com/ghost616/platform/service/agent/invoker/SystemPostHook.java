package com.ghost616.platform.service.agent.invoker;

import com.ghost616.platform.service.hook.SystemHook;

/**
 * 系统后置 HOOK 标记接口，继承 {@link SystemHook}，拥有执行顺序控制能力。
 * 标记此接口的 Bean 将被 {@code ChatService.initSystemHooks()} 识别并加入 {@code systemPostHooks} 列表，
 * 在每次触发 HOOK 时最后执行。
 *
 * @author ghost616
 */
public interface SystemPostHook extends SystemHook {
}
