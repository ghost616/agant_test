package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体消息查询日志数据，记录历史消息查询的关键信息。
 */
@Getter
@SuperBuilder
public class MessageQueryLogData extends ContextLogData {

    /** 会话 ID */
    private final String sessionId;

    /** 查询到的消息数量 */
    private final int messageCount;

    @Override
    public LogType logType() {
        return LogType.MESSAGE_QUERY;
    }
}
