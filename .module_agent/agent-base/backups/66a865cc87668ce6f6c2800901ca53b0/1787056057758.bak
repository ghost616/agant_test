package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.sendmessage.MessageDefinition;
import com.ghost616.agentbase.sendmessage.SendUserMessage;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.websocket.SessionConnectionRegistry;
import com.ghost616.platform.websocket.WebSocketPushService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMessageSenderTest {

    @Mock
    private WebSocketPushService pushService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private DefaultMessageSender newSender() {
        return new DefaultMessageSender(pushService);
    }

    private UserSession newUserSession(String sessionId) {
        User user = new User();
        user.setId(42L);
        return new UserSession(sessionId, user, System.currentTimeMillis());
    }

    private void awaitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("等待异步执行超时");
    }

    @Test
    void send_SEND_USER_MESSAGE_推送一次() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", "conv-1", "parent-1", "main-1");
        DefaultMessageSender sender = newSender();

        sender.send(message);

        verify(pushService, timeout(2000)).pushToSession(message);
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

    @Test
    void send_携带UserContext_异步线程恢复上下文() throws Exception {
        UserSession snapshot = newUserSession("usr-1");
        UserContext.set(snapshot);
        RecordingPushService recording = new RecordingPushService();
        DefaultMessageSender sender = new DefaultMessageSender(recording);

        sender.send(new SendUserMessage("child-1", "hello", "conv-1", "parent-1", "main-1"));

        awaitUntil(() -> recording.seenSession != null);
        assertEquals(snapshot, recording.seenSession);
    }

    @Test
    void send_无UserContext_异步线程无上下文() throws Exception {
        RecordingPushService recording = new RecordingPushService();
        DefaultMessageSender sender = new DefaultMessageSender(recording);

        sender.send(new SendUserMessage("child-1", "hello", "conv-1", "parent-1", "main-1"));

        awaitUntil(() -> recording.called);
        assertNull(recording.seenSession);
    }

    /**
     * 记录异步线程内 UserContext 状态的推送服务替身。
     */
    private static class RecordingPushService extends WebSocketPushService {

        volatile boolean called;
        volatile UserSession seenSession;

        RecordingPushService() {
            super(mock(SessionConnectionRegistry.class), new ObjectMapper());
        }

        @Override
        public void pushToSession(Object payload) {
            called = true;
            seenSession = UserContext.get();
        }
    }
}
