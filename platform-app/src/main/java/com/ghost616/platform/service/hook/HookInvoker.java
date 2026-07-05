package com.ghost616.platform.service.hook;


import com.ghost616.agentbase.dto.model.ChatChunk;
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
     * @param ctx   智能体执行上下文
     * @param chunk 聊天数据块
     */
    void execute(AgentExecutionContext ctx, ChatChunk chunk);
}
