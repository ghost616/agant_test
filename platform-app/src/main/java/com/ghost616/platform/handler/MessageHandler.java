package com.ghost616.platform.handler;

import com.ghost616.agentbase.sendmessage.MessageDefinition;

/**
 * sendmessage 消息处理器接口。实现类按 {@link #getMessageName()} 声明的消息类型订阅分发，
 * 由 {@link MessageDispatcher} 按 {@link MessageDefinition#getMessageName()} 查找并调用。
 */
public interface MessageHandler {

    /**
     * 返回该处理器订阅的消息类型名。
     *
     * @return 消息类型名（对应 {@link com.ghost616.agentbase.sendmessage.MessageName}）
     */
    String getMessageName();

    /**
     * 处理收到的消息。
     *
     * @param message 待处理的消息，messageName 与 {@link #getMessageName()} 一致
     */
    void handle(MessageDefinition message);
}