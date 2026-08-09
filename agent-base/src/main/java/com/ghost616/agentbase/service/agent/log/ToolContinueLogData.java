package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体工具执行后继续日志数据，记录工具结果持久化后继续对话的关键信息。
 */
@Getter
@SuperBuilder
public class ToolContinueLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 工具结果数量 */
    private final int resultCount;

    /** 已执行工具名称列表 */
    private final List<String> toolNames;

    @Override
    public LogType logType() {
        return LogType.TOOL_CONTINUE;
    }
}
