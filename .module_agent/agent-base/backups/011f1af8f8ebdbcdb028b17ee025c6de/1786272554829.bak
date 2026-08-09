package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体消息回退日志数据，记录回退到最后一条用户消息的关键信息。
 */
@Getter
@SuperBuilder
public class MessageRollbackLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 回退的消息数量 */
    private final int rollbackCount;

    @Override
    public LogType logType() {
        return LogType.MESSAGE_ROLLBACK;
    }
}
