package com.ghost616.platform.handler;

import com.ghost616.agentbase.sendmessage.MessageDefinition;
import com.ghost616.agentbase.sendmessage.MessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息分发器：实现 agent-base {@link MessageSender}，将应用内产生的消息按
 * {@link MessageDefinition#getMessageName()} 路由到对应的 {@link MessageHandler}。
 * 处理器集合通过构造器注入，对未注册的消息类型静默跳过（记录 debug 日志）。
 */
@Slf4j
@Component
public class MessageDispatcher implements MessageSender {

    private final Map<String, MessageHandler> handlersByMessageName;

    public MessageDispatcher(List<MessageHandler> handlers) {
        this.handlersByMessageName = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MessageHandler::getMessageName,
                        Function.identity(),
                        (existing, replacement) -> existing));
    }

    @Override
    public void send(MessageDefinition message) {
        if (message == null) {
            return;
        }
        MessageHandler handler = handlersByMessageName.get(message.getMessageName());
        if (handler == null) {
            log.debug("MessageDispatcher: 无处理器订阅消息类型 {}", message.getMessageName());
            return;
        }
        handler.handle(message);
        log.debug("MessageDispatcher: 消息类型 {} 已分发至 {}", message.getMessageName(), handler.getClass().getSimpleName());
    }
}