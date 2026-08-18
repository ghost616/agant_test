package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.sendmessage.MessageDefinition;
import com.ghost616.agentbase.sendmessage.SendUserMessage;
import com.ghost616.platform.websocket.WebSocketPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMessageSenderTest {

    @Mock
    private WebSocketPushService pushService;

    private DefaultMessageSender newSender() {
        return new DefaultMessageSender(pushService);
    }

    @Test
    void send_SEND_USER_MESSAGE_推送主会话与子会话() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", "conv-1", "parent-1", "main-1");
        DefaultMessageSender sender = newSender();

        sender.send(message);

        verify(pushService, timeout(2000)).pushToSession("main-1", message);
        verify(pushService, timeout(2000)).pushToSession("child-1", message);
    }

    @Test
    void send_SEND_USER_MESSAGE_主会话与子会话相同只推一次() {
        SendUserMessage message = new SendUserMessage("main-1", "hello", "conv-1", null, "main-1");
        DefaultMessageSender sender = newSender();

        sender.send(message);

        verify(pushService, timeout(2000)).pushToSession("main-1", message);
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void send_SEND_USER_MESSAGE_主会话为空只推子会话() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", "conv-1", "parent-1", null);
        DefaultMessageSender sender = newSender();

        sender.send(message);

        verify(pushService, timeout(2000)).pushToSession("child-1", message);
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void send_未知消息类型_静默跳过不推送() throws Exception {
        MessageDefinition unknown = () -> "UNKNOWN_TYPE";
        DefaultMessageSender sender = newSender();

        sender.send(unknown);

        Thread.sleep(200);
        verifyNoInteractions(pushService);
    }

    @Test
    void send_null消息_静默忽略() throws Exception {
        DefaultMessageSender sender = newSender();

        sender.send(null);

        Thread.sleep(100);
        verifyNoInteractions(pushService);
    }
}