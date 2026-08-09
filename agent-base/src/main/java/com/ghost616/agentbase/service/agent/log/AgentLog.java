package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;

/**
 * 智能体日志接口，定义日志写入契约。
 */
public interface AgentLog {

    /**
     * 记录一条智能体日志。
     *
     * @param context 智能体执行上下文
     * @param logData 日志数据
     */
    void addLog(AgentExecutionContext context, LogData logData);
}
