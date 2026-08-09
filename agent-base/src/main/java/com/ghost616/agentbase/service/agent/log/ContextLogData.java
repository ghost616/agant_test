package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 携带智能体执行上下文的日志数据基类。
 */
@Getter
@SuperBuilder
public abstract class ContextLogData extends LogData {

    /** 智能体执行上下文 */
    private final AgentExecutionContext context;
}
