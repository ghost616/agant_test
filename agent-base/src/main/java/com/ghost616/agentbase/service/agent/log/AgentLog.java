package com.ghost616.agentbase.service.agent.log;

/**
 * 智能体日志接口，定义日志写入契约。
 */
public interface AgentLog {

    /**
     * 记录一条智能体日志。
     *
     * @param logData 日志数据
     */
    void addLog(LogData logData);
}
