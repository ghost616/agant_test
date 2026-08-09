package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体工具执行日志数据，记录工具调用各分支的执行状态。
 */
@Getter
@SuperBuilder
public class ToolExecuteLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 工具调用 ID */
    private final String toolCallId;

    /** 工具调用名称 */
    private final String toolCallName;

    /** 工具调用参数 */
    private final String toolCallArguments;

    /** 工具类型：system/regular/builtin */
    private final String toolType;

    /** 队列状态：empty/executing/failed/error */
    private final String queueStatus;

    @Override
    public LogType logType() {
        return LogType.TOOL_EXECUTE;
    }
}
