package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体上下文刷新日志数据，记录上下文刷新操作的目标与会话信息。
 */
@Getter
@SuperBuilder
public class RefreshLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 刷新目标：HISTORY/SESSION_VARIABLES/CONVERSATION_VARIABLES/CHILD_SESSIONS */
    private final String refreshTarget;

    @Override
    public LogType logType() {
        return LogType.REFRESH;
    }
}
