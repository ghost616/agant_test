package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSubSessionCallbackTest {

    @Mock
    private SessionMapper sessionMapper;

    private DefaultSubSessionCallback callback;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        callback = new DefaultSubSessionCallback(sessionMapper);
    }

    @Test
    void executeShouldBlockAndReturnResult() throws Exception {
        Long sessionId = 100L;
        Long parentSessionId = 10L;
        String userMessage = "test message";
        Message expectedMessage = Message.builder().role("assistant").content("response").build();

        Session session = mock(Session.class);
        when(session.getParentSessionId()).thenReturn(parentSessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        CompletableFuture<Message> futureResult = CompletableFuture.supplyAsync(
                () -> callback.execute(sessionId, userMessage), executor);

        DefaultSubSessionCallback.SubSessionData data = waitForMapEntry(parentSessionId);
        assertNotNull(data);
        assertEquals(sessionId, data.getChildSessionId());
        assertEquals(userMessage, data.getUserMessage());

        data.getMessageResult().complete(expectedMessage);

        Message actual = futureResult.get(3, TimeUnit.SECONDS);
        assertEquals(expectedMessage, actual);
    }

    @Test
    void executeWithNullParentSessionIdReturnsNull() {
        Long sessionId = 200L;

        Session session = mock(Session.class);
        when(session.getParentSessionId()).thenReturn(null);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        Message result = callback.execute(sessionId, "no parent");
        assertNull(result);
    }

    @Test
    void executeWithSessionNotFoundReturnsNull() {
        Long sessionId = 300L;
        when(sessionMapper.selectById(sessionId)).thenReturn(null);

        Message result = callback.execute(sessionId, "not found");
        assertNull(result);
    }

    @Test
    void executeShouldRemoveEntryAfterCompletion() throws Exception {
        Long sessionId = 400L;
        Long parentSessionId = 40L;

        Session session = mock(Session.class);
        when(session.getParentSessionId()).thenReturn(parentSessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        CompletableFuture<Message> futureResult = CompletableFuture.supplyAsync(
                () -> callback.execute(sessionId, "cleanup test"), executor);

        DefaultSubSessionCallback.SubSessionData data = waitForMapEntry(parentSessionId);
        assertNotNull(data);

        data.getMessageResult().complete(Message.builder().role("assistant").content("done").build());
        futureResult.get(3, TimeUnit.SECONDS);

        assertNull(callback.getSubSessionData(parentSessionId));
    }

    @Test
    void getSubSessionDataShouldReturnNullWhenNoData() {
        assertNull(callback.getSubSessionData(999L));
    }

    @Test
    void getSubSessionDataShouldReturnCorrectData() throws Exception {
        Long sessionId = 600L;
        Long parentSessionId = 60L;
        String userMessage = "data test";
        Message expectedMessage = Message.builder().role("user").content("hello").build();

        Session session = mock(Session.class);
        when(session.getParentSessionId()).thenReturn(parentSessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        CompletableFuture<Message> futureResult = CompletableFuture.supplyAsync(
                () -> callback.execute(sessionId, userMessage), executor);

        DefaultSubSessionCallback.SubSessionData data = waitForMapEntry(parentSessionId);
        assertNotNull(data);
        assertEquals(sessionId, data.getChildSessionId());
        assertEquals(userMessage, data.getUserMessage());
        assertFalse(data.getMessageResult().isDone());

        data.getMessageResult().complete(expectedMessage);
        Message actual = futureResult.get(3, TimeUnit.SECONDS);
        assertEquals(expectedMessage, actual);
    }

    @Test
    void executeShouldThrowOnInterruption() {
        Long sessionId = 700L;
        Long parentSessionId = 70L;

        Session session = mock(Session.class);
        when(session.getParentSessionId()).thenReturn(parentSessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        Thread testThread = new Thread(() -> {
            try {
                callback.execute(sessionId, "interrupt test");
                fail("Should have thrown exception");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("interrupted"));
            }
        });
        testThread.start();

        DefaultSubSessionCallback.SubSessionData data = waitForMapEntry(parentSessionId);
        assertNotNull(data);

        testThread.interrupt();
        try {
            testThread.join(3000);
        } catch (InterruptedException e) {
            fail("Test thread join interrupted");
        }
    }

    private DefaultSubSessionCallback.SubSessionData waitForMapEntry(Long key) {
        for (int i = 0; i < 50; i++) {
            DefaultSubSessionCallback.SubSessionData data = callback.getSubSessionData(key);
            if (data != null) {
                return data;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
