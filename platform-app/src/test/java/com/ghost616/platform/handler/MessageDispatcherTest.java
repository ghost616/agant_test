package com.ghost616.platform.handler;

import com.ghost616.agentbase.sendmessage.ConversationIdMessage;
import com.ghost616.agentbase.sendmessage.MessageName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageDispatcherTest {

    @Test
    void send_按消息名分发到对应处理器() {
        MessageHandler handler = mock(MessageHandler.class);
        when(handler.getMessageName()).thenReturn(MessageName.CONVERSATION_ID);

        MessageDispatcher dispatcher = new MessageDispatcher(List.of(handler));
        ConversationIdMessage message = new ConversationIdMessage("session-1", "conv-1");

        dispatcher.send(message);

        verify(handler, times(1)).handle(message);
    }

    @Test
    void send_无对应处理器时静默跳过() {
        MessageHandler handler = mock(MessageHandler.class);
        when(handler.getMessageName()).thenReturn(MessageName.HISTORY_MESSAGE);

        MessageDispatcher dispatcher = new MessageDispatcher(List.of(handler));
        ConversationIdMessage message = new ConversationIdMessage("session-1", "conv-1");

        assertDoesNotThrow(() -> dispatcher.send(message));
        verify(handler, never()).handle(any());
    }

    @Test
    void send_null消息静默跳过() {
        MessageHandler handler = mock(MessageHandler.class);
        when(handler.getMessageName()).thenReturn(MessageName.CONVERSATION_ID);

        MessageDispatcher dispatcher = new MessageDispatcher(List.of(handler));

        assertDoesNotThrow(() -> dispatcher.send(null));
        verify(handler, never()).handle(any());
    }
}
