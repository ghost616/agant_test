package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.FinishReason;
import com.ghost616.agentbase.service.agent.CacheSessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultChatDataCacheProviderTest {

    private DefaultChatDataCacheProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultChatDataCacheProvider();
    }

    private ChatChunk chunk(String delta, FinishReason finishReason) {
        return ChatChunk.builder().delta(delta).finishReason(finishReason).build();
    }

    @Test
    void createCache_正常创建_返回缓存ID并建立双向映射() {
        String cacheId = provider.createCache("session-1", "conv-1");

        assertNotNull(cacheId);
        assertTrue(provider.cacheExists(cacheId));
        assertTrue(provider.cacheExists("session-1", "conv-1"));
        assertEquals(cacheId, provider.getCacheId("session-1", "conv-1"));
    }

    @Test
    void createCache_相同键已存在_返回null() {
        String cacheId = provider.createCache("session-1", "conv-1");
        assertNotNull(cacheId);

        String duplicate = provider.createCache("session-1", "conv-1");

        assertNull(duplicate);
        assertEquals(cacheId, provider.getCacheId("session-1", "conv-1"));
    }

    @Test
    void createCache_不同键_均可创建() {
        String cacheId1 = provider.createCache("session-1", "conv-1");
        String cacheId2 = provider.createCache("session-1", "conv-2");

        assertNotNull(cacheId1);
        assertNotNull(cacheId2);
        assertNotEquals(cacheId1, cacheId2);
        assertTrue(provider.cacheExists("session-1", "conv-1"));
        assertTrue(provider.cacheExists("session-1", "conv-2"));
    }

    @Test
    void cacheExists_byCacheId_不存在时返回false() {
        assertFalse(provider.cacheExists("non-existent"));
    }

    @Test
    void cacheExists_bySessionKey_不存在时返回false() {
        assertFalse(provider.cacheExists("session-1", "conv-1"));
    }

    @Test
    void isCacheDone_缓存不存在_返回false() {
        assertFalse(provider.isCacheDone("non-existent"));
    }

    @Test
    void isCacheDone_初始状态_返回false() {
        String cacheId = provider.createCache("session-1", "conv-1");

        assertFalse(provider.isCacheDone(cacheId));
    }

    @Test
    void getCacheId_不存在_返回null() {
        assertNull(provider.getCacheId("session-1", "conv-1"));
    }

    @Test
    void getCacheIdsBySessionId_无匹配会话_返回空列表() {
        provider.createCache("session-1", "conv-1");

        List<String> cacheIds = provider.getCacheIdsBySessionId("other-session");

        assertNotNull(cacheIds);
        assertTrue(cacheIds.isEmpty());
    }

    @Test
    void getCacheIdsBySessionId_匹配会话_返回全部缓存ID() {
        String id1 = provider.createCache("session-1", "conv-1");
        String id2 = provider.createCache("session-1", "conv-2");
        provider.createCache("session-2", "conv-1");

        List<String> cacheIds = provider.getCacheIdsBySessionId("session-1");

        assertEquals(2, cacheIds.size());
        assertTrue(cacheIds.contains(id1));
        assertTrue(cacheIds.contains(id2));
    }

    @Test
    void getCacheIdsBySessionId_按创建时间升序返回() {
        String first = provider.createCache("session-1", "conv-1");
        String second = provider.createCache("session-1", "conv-2");
        String third = provider.createCache("session-1", "conv-3");

        List<String> cacheIds = provider.getCacheIdsBySessionId("session-1");

        assertEquals(3, cacheIds.size());
        assertEquals(first, cacheIds.get(0));
        assertEquals(second, cacheIds.get(1));
        assertEquals(third, cacheIds.get(2));
    }

    @Test
    void getCacheIdsBySessionId_不包含其他会话的缓存() {
        String id = provider.createCache("session-1", "conv-1");
        provider.createCache("session-2", "conv-9");

        List<String> cacheIds = provider.getCacheIdsBySessionId("session-1");

        assertEquals(1, cacheIds.size());
        assertEquals(id, cacheIds.get(0));
    }

    @Test
    void getCacheIdsBySessionId_已删除缓存不返回() {
        String id = provider.createCache("session-1", "conv-1");
        provider.createCache("session-1", "conv-2");
        provider.removeCache(id);

        List<String> cacheIds = provider.getCacheIdsBySessionId("session-1");

        assertEquals(1, cacheIds.size());
        assertEquals(provider.getCacheId("session-1", "conv-2"), cacheIds.get(0));
    }

    @Test
    void getCacheSessionInfo_缓存存在_返回会话信息() {
        String cacheId = provider.createCache("session-1", "conv-1");

        CacheSessionInfo info = provider.getCacheSessionInfo(cacheId);

        assertNotNull(info);
        assertEquals("session-1", info.sessionId());
        assertEquals("conv-1", info.conversationId());
    }

    @Test
    void getCacheSessionInfo_缓存不存在_返回null() {
        assertNull(provider.getCacheSessionInfo("non-existent"));
    }

    @Test
    void getMaxChunkIndex_无缓存_返回负一() {
        assertEquals(-1, provider.getMaxChunkIndex("non-existent"));
    }

    @Test
    void getMaxChunkIndex_无数据_返回负一() {
        String cacheId = provider.createCache("session-1", "conv-1");

        assertEquals(-1, provider.getMaxChunkIndex(cacheId));
    }

    @Test
    void appendChunk_无finishReason_done保持false() {
        String cacheId = provider.createCache("session-1", "conv-1");

        provider.appendChunk(cacheId, chunk("hello", null));

        assertFalse(provider.isCacheDone(cacheId));
        assertEquals(0, provider.getMaxChunkIndex(cacheId));
    }

    @Test
    void appendChunk_有finishReason_done切换为true() {
        String cacheId = provider.createCache("session-1", "conv-1");

        provider.appendChunk(cacheId, chunk("hello", null));
        provider.appendChunk(cacheId, chunk("world", FinishReason.STOP));

        assertTrue(provider.isCacheDone(cacheId));
        assertEquals(1, provider.getMaxChunkIndex(cacheId));
    }

    @Test
    void appendChunk_缓存不存在_静默忽略() {
        provider.appendChunk("non-existent", chunk("hello", FinishReason.STOP));

        assertFalse(provider.cacheExists("non-existent"));
    }

    @Test
    void appendChunk_已结束后_不再追加块() {
        String cacheId = provider.createCache("session-1", "conv-1");
        provider.appendChunk(cacheId, chunk("end", FinishReason.STOP));

        provider.appendChunk(cacheId, chunk("late", null));
        provider.appendChunk(cacheId, chunk("late2", FinishReason.STOP));

        assertTrue(provider.isCacheDone(cacheId));
        assertEquals(0, provider.getMaxChunkIndex(cacheId));
        assertEquals(1, provider.getChunks(cacheId, 0, 5).size());
        assertEquals("end", provider.getChunks(cacheId, 0, 0).get(0).getDelta());
    }

    @Test
    void removeCache_删除后caches与keyMap均清理() {
        String cacheId = provider.createCache("session-1", "conv-1");
        provider.appendChunk(cacheId, chunk("hello", null));

        provider.removeCache(cacheId);

        assertFalse(provider.cacheExists(cacheId));
        assertFalse(provider.cacheExists("session-1", "conv-1"));
        assertNull(provider.getCacheId("session-1", "conv-1"));
        assertNull(provider.getCacheSessionInfo(cacheId));
        assertEquals(-1, provider.getMaxChunkIndex(cacheId));
    }

    @Test
    void removeCache_缓存不存在_无异常() {
        assertDoesNotThrow(() -> provider.removeCache("non-existent"));
    }

    @Test
    void getChunks_缓存不存在_返回空列表() {
        List<ChatChunk> chunks = provider.getChunks("non-existent", 0, 2);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void getChunks_含两端边界_返回对应范围新列表() {
        String cacheId = provider.createCache("session-1", "conv-1");
        for (int i = 0; i < 5; i++) {
            provider.appendChunk(cacheId, chunk("c" + i, null));
        }

        List<ChatChunk> chunks = provider.getChunks(cacheId, 1, 3);

        assertEquals(3, chunks.size());
        assertEquals("c1", chunks.get(0).getDelta());
        assertEquals("c2", chunks.get(1).getDelta());
        assertEquals("c3", chunks.get(2).getDelta());
    }

    @Test
    void getChunks_全量范围_返回全部块() {
        String cacheId = provider.createCache("session-1", "conv-1");
        for (int i = 0; i < 3; i++) {
            provider.appendChunk(cacheId, chunk("c" + i, null));
        }

        List<ChatChunk> chunks = provider.getChunks(cacheId, 0, 2);

        assertEquals(3, chunks.size());
        assertEquals("c0", chunks.get(0).getDelta());
        assertEquals("c2", chunks.get(2).getDelta());
    }

    @Test
    void getChunks_返回新列表_修改不影响缓存() {
        String cacheId = provider.createCache("session-1", "conv-1");
        provider.appendChunk(cacheId, chunk("c0", null));

        List<ChatChunk> chunks = provider.getChunks(cacheId, 0, 0);
        chunks.clear();

        assertEquals(1, provider.getChunks(cacheId, 0, 0).size());
        assertEquals(0, provider.getMaxChunkIndex(cacheId));
    }

    @Test
    void getChunks_startIndex小于0_返回空列表() {
        String cacheId = provider.createCache("session-1", "conv-1");
        provider.appendChunk(cacheId, chunk("c0", null));

        List<ChatChunk> chunks = provider.getChunks(cacheId, -1, 0);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void getChunks_endIndex小于startIndex_返回空列表() {
        String cacheId = provider.createCache("session-1", "conv-1");
        for (int i = 0; i < 5; i++) {
            provider.appendChunk(cacheId, chunk("c" + i, null));
        }

        List<ChatChunk> chunks = provider.getChunks(cacheId, 3, 1);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void getChunks_startIndex越界_返回空列表() {
        String cacheId = provider.createCache("session-1", "conv-1");
        for (int i = 0; i < 3; i++) {
            provider.appendChunk(cacheId, chunk("c" + i, null));
        }

        List<ChatChunk> chunks = provider.getChunks(cacheId, 3, 5);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void getChunks_endIndex超过最大序号_截断到最大序号() {
        String cacheId = provider.createCache("session-1", "conv-1");
        for (int i = 0; i < 5; i++) {
            provider.appendChunk(cacheId, chunk("c" + i, null));
        }

        List<ChatChunk> chunks = provider.getChunks(cacheId, 1, 99);

        assertEquals(4, chunks.size());
        assertEquals("c1", chunks.get(0).getDelta());
        assertEquals("c4", chunks.get(3).getDelta());
    }
}
