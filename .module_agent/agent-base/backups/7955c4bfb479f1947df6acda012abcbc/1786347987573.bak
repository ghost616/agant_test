package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天数据缓存管理器，封装流式聊天片段的缓存生命周期与读取逻辑。
 * 通过 {@link ChatDataCacheProvider} 与具体数据访问层解耦。
 */
@Slf4j
public class ChatDataCacheManager {

    private final ChatDataCacheProvider provider;

    public ChatDataCacheManager(ChatDataCacheProvider provider) {
        this.provider = provider;
    }

    /**
     * 为指定会话与对话创建缓存并返回缓存 ID。
     * 当缓存已存在时抛出 {@link BusinessException}。
     *
     * @param sessionId      会话 ID
     * @param conversationId 对话 ID
     * @return 缓存 ID
     * @throws BusinessException 缓存已存在
     */
    public String startCache(String sessionId, String conversationId) {
        String cacheId = provider.createCache(sessionId, conversationId);
        if (cacheId == null) {
            log.warn("缓存已存在, sessionId={}, conversationId={}", sessionId, conversationId);
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "缓存已存在");
        }
        return cacheId;
    }

    /**
     * 将聊天块追加到缓存。
     *
     * @param cacheId 缓存 ID
     * @param chunk   聊天块
     */
    public void appendChunk(String cacheId, ChatChunk chunk) {
        provider.appendChunk(cacheId, chunk);
    }

    /**
     * 删除缓存。
     *
     * @param cacheId 缓存 ID
     */
    public void removeCache(String cacheId) {
        provider.removeCache(cacheId);
    }

    /**
     * 从缓存读取从 startIndex 开始的所有聊天块并转为 SSE 流返回。
     * 缓存不存在、无数据或 startIndex 大于最大序号时抛出 {@link BusinessException}。
     *
     * @param cacheId    缓存 ID
     * @param startIndex 起始序号
     * @return SSE 聊天块流
     * @throws BusinessException 缓存不存在、无数据或起始序号越界
     */
    public Flux<ServerSentEvent<ChatChunk>> getStream(String cacheId, int startIndex) {
        if (!provider.cacheExists(cacheId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "缓存不存在");
        }
        int maxIndex = provider.getMaxChunkIndex(cacheId);
        if (maxIndex < 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "缓存无数据");
        }
        if (startIndex > maxIndex) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "起始序号超过最大序号");
        }
        List<ChatChunk> chunks = provider.getChunks(cacheId, startIndex, maxIndex);
        return Flux.fromIterable(chunks)
                .map(chunk -> ServerSentEvent.<ChatChunk>builder().data(chunk).build());
    }
}
