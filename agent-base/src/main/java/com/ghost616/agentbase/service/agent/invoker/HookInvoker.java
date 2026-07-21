package com.ghost616.agentbase.service.agent.invoker;


import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;


/**
 * HOOK 执行契约接口。
 *
 * @author ghost616
 */
public interface HookInvoker {

    /**
     * 获取该 HOOK 生效的阶段。
     *
     * @return HOOK 生效阶段
     */
    HookPhase getPhase();

    /**
     * 执行 HOOK。
     *
     * @param ctx  智能体执行上下文
     * @param data HOOK 数据载体
     */
    void execute(AgentExecutionContext ctx, HookData data);
}
