package com.ghost616.platform.service.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.CacheSessionInfo;
import com.ghost616.agentbase.service.agent.ChatDataCacheProvider;
import lombok.RequiredArgsConstructor;

/**
 * 聊天数据缓存默认实现，基于内存 ConcurrentHashMap 提供流式聊天片段的缓存管理。
 * 以 cacheId 为键存储缓存条目，并提供 sessionId:conversationId 到 cacheId 的反向映射。
 */
@RequiredArgsConstructor
public class DefaultChatDataCacheProvider implements ChatDataCacheProvider {

    private final ConcurrentHashMap<String, CacheEntry> caches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> keyMap = new ConcurrentHashMap<>();

    /**
     * 缓存条目，承载聊天块列表、结束标记及所属会话与对话 ID。
     */
    private static class CacheEntry {
        private final List<ChatChunk> chunks = new ArrayList<>();
        private boolean done;
        private final String sessionId;
        private final String conversationId;

        private CacheEntry(String sessionId, String conversationId) {
            this.sessionId = sessionId;
            this.conversationId = conversationId;
        }
    }

    /**
     * 组装 sessionId 与 conversationId 的组合键。
     */
    private static String key(String sessionId, String conversationId) {
        return sessionId + ":" + conversationId;
    }

    @Override
    public String createCache(String sessionId, String conversationId) {
        String cacheKey = key(sessionId, conversationId);
        if (keyMap.containsKey(cacheKey)) {
            return null;
        }
        String cacheId = UUID.randomUUID().toString();
        caches.put(cacheId, new CacheEntry(sessionId, conversationId));
        keyMap.put(cacheKey, cacheId);
        return cacheId;
    }

    @Override
    public boolean cacheExists(String cacheId) {
        return caches.containsKey(cacheId);
    }

    @Override
    public boolean cacheExists(String sessionId, String conversationId) {
        return keyMap.containsKey(key(sessionId, conversationId));
    }

    @Override
    public boolean isCacheDone(String cacheId) {
        CacheEntry entry = caches.get(cacheId);
        return entry != null && entry.done;
    }

    @Override
    public String getCacheId(String sessionId, String conversationId) {
        return keyMap.get(key(sessionId, conversationId));
    }

    @Override
    public CacheSessionInfo getCacheSessionInfo(String cacheId) {
        CacheEntry entry = caches.get(cacheId);
        if (entry == null) {
            return null;
        }
        return new CacheSessionInfo(entry.sessionId, entry.conversationId);
    }

    @Override
    public int getMaxChunkIndex(String cacheId) {
        CacheEntry entry = caches.get(cacheId);
        if (entry == null) {
            return -1;
        }
        return entry.chunks.size() - 1;
    }

    @Override
    public void appendChunk(String cacheId, ChatChunk chunk) {
        CacheEntry entry = caches.get(cacheId);
        if (entry == null) {
            return;
        }
        synchronized (entry) {
            if (entry.done) {
                return;
            }
            entry.chunks.add(chunk);
            if (chunk.getFinishReason() != null) {
                entry.done = true;
            }
        }
    }

    @Override
    public void removeCache(String cacheId) {
        CacheEntry entry = caches.remove(cacheId);
        if (entry != null) {
            keyMap.remove(key(entry.sessionId, entry.conversationId));
        }
    }

    @Override
    public List<ChatChunk> getChunks(String cacheId, int startIndex, int endIndex) {
        CacheEntry entry = caches.get(cacheId);
        if (entry == null) {
            return List.of();
        }
        if (startIndex < 0 || endIndex < startIndex || startIndex >= entry.chunks.size()) {
            return List.of();
        }
        int safeEndIndex = Math.min(endIndex, entry.chunks.size() - 1);
        return new ArrayList<>(entry.chunks.subList(startIndex, safeEndIndex + 1));
    }
}
