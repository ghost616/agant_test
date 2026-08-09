package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体会话级错误日志数据，用于无智能体执行上下文（AgentExecutionContext）时记录错误信息。
 */
@Getter
@SuperBuilder
public class SessionErrorLogData extends SessionLogData {

    /** 错误码 */
    private final String errorCode;

    /** 错误消息 */
    private final String message;

    /** 异常对象 */
    private final Throwable exception;

    @Override
    public LogType logType() {
        return LogType.ERROR_LOG;
    }
}
