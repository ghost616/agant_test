package com.ghost616.platform.handler;

import com.ghost616.agentbase.sendmessage.ConversationIdMessage;
import com.ghost616.agentbase.sendmessage.MessageDefinition;
import com.ghost616.agentbase.sendmessage.MessageName;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * ConversationIdMessage 处理器。
 * <p>
 * 收到 {@link ConversationIdMessage} 时，将消息中的 conversationId 同步到
 * {@link AgentContextManager#handleConversationIdMessage(ConversationIdMessage)}。
 * AgentContextManager 通过 ObjectProvider 惰性获取，避免与 AgentAssembler 装配产生循环依赖。
 * <p>
 * 重入保护：{@link AgentContextManager#handleConversationIdMessage} 内部会再次调用
 * mutator.setConversationId() 并再次发送 ConversationIdMessage，若本处理器直接回放将造成无限递归。
 * 因此使用 ThreadLocal 标记处理中状态，处理期间嵌套到达的同类型消息直接跳过。
 */
@Component
@RequiredArgsConstructor
public class ConversationIdMessageHandler implements MessageHandler {

    private final ObjectProvider<AgentContextManager> agentContextManagerProvider;

    private final ThreadLocal<Boolean> handling = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public String getMessageName() {
        return MessageName.CONVERSATION_ID;
    }

    @Override
    public void handle(MessageDefinition message) {
        if (!(message instanceof ConversationIdMessage conversationIdMessage)) {
            return;
        }
        if (Boolean.TRUE.equals(handling.get())) {
            return;
        }
        handling.set(Boolean.TRUE);
        try {
            AgentContextManager agentContextManager = agentContextManagerProvider.getObject();
            agentContextManager.handleConversationIdMessage(conversationIdMessage);
        } finally {
            handling.remove();
        }
    }
}
