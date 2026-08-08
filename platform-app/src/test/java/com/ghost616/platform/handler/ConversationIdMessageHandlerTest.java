package com.ghost616.platform.handler;

import com.ghost616.agentbase.sendmessage.ConversationIdMessage;
import com.ghost616.agentbase.sendmessage.MessageName;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationIdMessageHandlerTest {

    @Mock
    private ObjectProvider<AgentContextManager> agentContextManagerProvider;

    @Mock
    private AgentContextManager agentContextManager;

    private ConversationIdMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ConversationIdMessageHandler(agentContextManagerProvider);
    }

    @Test
    void getMessageName_返回CONVERSATION_ID() {
        assertEquals(MessageName.CONVERSATION_ID, handler.getMessageName());
    }

    @Test
    void handle_收到ConversationIdMessage时调用handleConversationIdMessage() {
        ConversationIdMessage message = new ConversationIdMessage("session-1", "conv-1");
        when(agentContextManagerProvider.getObject()).thenReturn(agentContextManager);

        handler.handle(message);

        verify(agentContextManager, times(1)).handleConversationIdMessage(message);
    }

    @Test
    void handle_收到非ConversationIdMessage时不调用() {
        handler.handle(() -> MessageName.HISTORY_MESSAGE);

        verify(agentContextManagerProvider, never()).getObject();
        verifyNoInteractions(agentContextManager);
    }

    @Test
    void handle_重入时跳过嵌套调用避免递归() {
        ConversationIdMessage message = new ConversationIdMessage("session-1", "conv-1");
        when(agentContextManagerProvider.getObject()).thenReturn(agentContextManager);
        doAnswer(invocation -> {
            handler.handle(message);
            return null;
        }).when(agentContextManager).handleConversationIdMessage(any(ConversationIdMessage.class));

        handler.handle(message);

        verify(agentContextManager, times(1)).handleConversationIdMessage(any(ConversationIdMessage.class));
    }
}
