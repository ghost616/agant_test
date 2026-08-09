package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.sendmessage.SessionMessage;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体消息处理日志数据，记录外部消息处理操作的会话消息信息。
 */
@Getter
@SuperBuilder
public class HandleMessageLogData extends ContextLogData {

    /** 被处理的会话消息 */
    private final SessionMessage sessionMessage;

    @Override
    public LogType logType() {
        return LogType.HANDLE_MESSAGE;
    }
}
