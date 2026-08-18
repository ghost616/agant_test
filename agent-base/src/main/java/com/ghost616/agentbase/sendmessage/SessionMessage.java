package com.ghost616.agentbase.sendmessage;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class SessionMessage implements MessageDefinition {

    private String sessionId;

    /** 对话 ID（消息所属对话标识） */
    private String conversationId;

    /**
     * 父会话链（有序列表），语义：第一个 = 直接父会话 ID，最后一个 = 主会话 ID
     * （沿父会话链向上追溯到无父会话的根会话），中间为各层父会话。
     * 主会话自身无父链时为 null 或空列表。
     */
    private List<String> parentSessionIds;
}
