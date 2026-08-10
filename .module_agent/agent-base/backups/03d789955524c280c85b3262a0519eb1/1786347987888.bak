package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatDataCacheManagerTest {

    @Mock
    private ChatDataCacheProvider provider;

    private ChatDataCacheManager manager;

    private final String sessionId = "session-1";
    private final String conversationId = "conversation-1";
    private final String cacheId = "cache-1";

    @BeforeEach
    void setUp() {
        manager = new ChatDataCacheManager(provider);
    }

    private ChatChunk chunk(int index) {
        return ChatChunk.builder().index(index).delta("delta-" + index).build();
    }

    @Test
    void startCache_shouldReturnCacheId() {
        when(provider.createCache(sessionId, conversationId)).thenReturn(cacheId);

        String actual = manager.startCache(sessionId, conversationId);

        assertEquals(cacheId, actual);
        verify(provider).createCache(sessionId, conversationId);
    }

    @Test
    void startCache_whenCacheAlreadyExists_shouldThrowDuplicateKey() {
        when(provider.createCache(sessionId, conversationId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> manager.startCache(sessionId, conversationId));

        assertEquals(ErrorCode.DUPLICATE_KEY, ex.getErrorCode());
        verify(provider).createCache(sessionId, conversationId);
    }

    @Test
    void appendChunk_shouldDelegateToProvider() {
        ChatChunk chunk = chunk(0);

        manager.appendChunk(cacheId, chunk);

        verify(provider).appendChunk(cacheId, chunk);
    }

    @Test
    void removeCache_shouldDelegateToProvider() {
        manager.removeCache(cacheId);

        verify(provider).removeCache(cacheId);
    }

    @Test
    void getStream_shouldReturnSseEventsForChunkRange() {
        List<ChatChunk> chunks = List.of(chunk(0), chunk(1), chunk(2));
        when(provider.cacheExists(cacheId)).thenReturn(true);
        when(provider.getMaxChunkIndex(cacheId)).thenReturn(2);
        when(provider.getChunks(cacheId, 0, 2)).thenReturn(chunks);

        Flux<ServerSentEvent<ChatChunk>> flux = manager.getStream(cacheId, 0);

        StepVerifier.create(flux)
                .assertNext(ev -> assertEquals(0, ev.data().getIndex()))
                .assertNext(ev -> assertEquals(1, ev.data().getIndex()))
                .assertNext(ev -> assertEquals(2, ev.data().getIndex()))
                .verifyComplete();
        verify(provider).getChunks(cacheId, 0, 2);
    }

    @Test
    void getStream_whenStartIndexEqualsMaxIndex_shouldReturnSingleEvent() {
        List<ChatChunk> chunks = List.of(chunk(1));
        when(provider.cacheExists(cacheId)).thenReturn(true);
        when(provider.getMaxChunkIndex(cacheId)).thenReturn(1);
        when(provider.getChunks(cacheId, 1, 1)).thenReturn(chunks);

        StepVerifier.create(manager.getStream(cacheId, 1))
                .assertNext(ev -> assertEquals(1, ev.data().getIndex()))
                .verifyComplete();
        verify(provider).getChunks(cacheId, 1, 1);
    }

    @Test
    void getStream_whenCacheNotExists_shouldThrowNotFound() {
        when(provider.cacheExists(cacheId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> manager.getStream(cacheId, 0));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(provider, never()).getMaxChunkIndex(any());
        verify(provider, never()).getChunks(any(), anyInt(), anyInt());
    }

    @Test
    void getStream_whenNoData_shouldThrowNotFound() {
        when(provider.cacheExists(cacheId)).thenReturn(true);
        when(provider.getMaxChunkIndex(cacheId)).thenReturn(-1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> manager.getStream(cacheId, 0));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(provider, never()).getChunks(any(), anyInt(), anyInt());
    }

    @Test
    void getStream_whenStartIndexExceedsMaxIndex_shouldThrowParamInvalid() {
        when(provider.cacheExists(cacheId)).thenReturn(true);
        when(provider.getMaxChunkIndex(cacheId)).thenReturn(3);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> manager.getStream(cacheId, 4));

        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        verify(provider, never()).getChunks(any(), anyInt(), anyInt());
    }
}
