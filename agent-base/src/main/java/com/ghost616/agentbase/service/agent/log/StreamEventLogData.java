package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体流式事件日志数据，记录流式响应过程中的关键事件。
 */
@Getter
@SuperBuilder
public class StreamEventLogData extends ContextLogData {

    /** 事件类型（ToolCallDetected / StreamComplete / StreamCancelled） */
    private final String eventType;

    /** 是否包含工具调用 */
    private final Boolean hasToolCalls;

    @Override
    public LogType logType() {
        return LogType.STREAM_EVENT;
    }
}
