package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体消息发送日志数据，记录向子会话发送用户消息的关键信息。
 */
@Getter
@SuperBuilder
public class SendMessageLogData extends LogData {

    /** 父会话 ID */
    private final String parentSessionId;

    /** 子会话 ID */
    private final String childSessionId;

    /** 消息内容 */
    private final String content;

    /** 模型 ID */
    private final String modelId;

    /** 是否启用思考模式 */
    private final Boolean thinking;

    @Override
    public LogType logType() {
        return LogType.SEND_MESSAGE;
    }
}
