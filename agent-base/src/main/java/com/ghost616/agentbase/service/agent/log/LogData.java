package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogLevel;
import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体日志数据抽象基类。
 */
@Getter
@SuperBuilder
public abstract class LogData {

    /** 日志级别 */
    private final LogLevel logLevel;

    /**
     * 日志类型。
     *
     * @return 日志类型枚举
     */
    public abstract LogType logType();
}
