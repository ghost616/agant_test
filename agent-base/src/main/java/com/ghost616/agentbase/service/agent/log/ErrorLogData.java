package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体错误日志数据，记录智能体执行过程中的错误信息。
 */
@Getter
@SuperBuilder
public class ErrorLogData extends ContextLogData {

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
