package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.enums.FinishReason;
import com.ghost616.agentbase.enums.LogLevel;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.service.agent.log.ChatCacheLogData;
import com.ghost616.agentbase.service.agent.log.LogData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聊天数据缓存管理器，封装流式聊天片段的缓存生命周期与读取逻辑。
 * 通过 {@link ChatDataCacheProvider} 与具体数据访问层解耦。
 */
@Slf4j
public class ChatDataCacheManager {

    /** 轮询间隔，用于在流式数据未结束时持续获取新增块 */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(1);

    /** 轮询无新数据超时时间，超时后生成 finishReason=ERROR 的结束块并终止流 */
    private static final long POLL_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    /** 缓存操作类型常量 */
    private static final String OPERATION_CACHE_START = "CACHE_START";
    private static final String OPERATION_CACHE_APPEND = "CACHE_APPEND";
    private static final String OPERATION_CACHE_REMOVE = "CACHE_REMOVE";
    private static final String OPERATION_CACHE_STREAM = "CACHE_STREAM";

    private final ChatDataCacheProvider provider;

    private AgentLog agentLog;

    private long pollTimeoutMs = POLL_TIMEOUT_MS;

    public ChatDataCacheManager(ChatDataCacheProvider provider) {
        this.provider = provider;
    }

    public void setAgentLog(AgentLog agentLog) {
        this.agentLog = agentLog;
    }

    /**
     * 设置轮询无新数据超时时间，默认使用 {@link #POLL_TIMEOUT_MS}。供测试缩短超时验证。
     *
     * @param pollTimeoutMs 超时毫秒数
     */
    void setPollTimeoutMs(long pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
    }

    /**
     * 按会话与对话返回缓存 ID。
     *
     * @param sessionId      会话 ID
     * @param conversationId 对话 ID
     * @return 缓存 ID，不存在时返回 null
     */
    public String getCacheId(String sessionId, String conversationId) {
        return provider.getCacheId(sessionId, conversationId);
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
        if (provider.cacheExists(sessionId, conversationId)) {
            log.warn("缓存已存在, sessionId={}, conversationId={}", sessionId, conversationId);
            addCacheLog(LogLevel.ERROR, OPERATION_CACHE_START, null, sessionId, conversationId);
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "缓存已存在");
        }
        String cacheId = provider.createCache(sessionId, conversationId);
        if (cacheId == null) {
            log.warn("创建缓存失败, sessionId={}, conversationId={}", sessionId, conversationId);
            addCacheLog(LogLevel.ERROR, OPERATION_CACHE_START, null, sessionId, conversationId);
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "缓存已存在");
        }
        addCacheLog(LogLevel.INFO, OPERATION_CACHE_START, cacheId, sessionId, conversationId);
        return cacheId;
    }

    /**
     * 将聊天块追加到缓存。
     * 缓存不存在或缓存已结束时抛出 {@link BusinessException}。
     *
     * @param cacheId 缓存 ID
     * @param chunk   聊天块
     * @throws BusinessException 缓存不存在或缓存已结束
     */
    public void appendChunk(String cacheId, ChatChunk chunk) {
        if (!provider.cacheExists(cacheId)) {
            addCacheLog(LogLevel.ERROR, OPERATION_CACHE_APPEND, cacheId, null, null);
            throw new BusinessException(ErrorCode.NOT_FOUND, "缓存不存在");
        }
        if (provider.isCacheDone(cacheId)) {
            addCacheLog(LogLevel.ERROR, OPERATION_CACHE_APPEND, cacheId, null, null);
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缓存已结束");
        }
        provider.appendChunk(cacheId, chunk);
        if (agentLog != null) {
            boolean isFirstChunk = (chunk.getIndex() != null && chunk.getIndex() == 0)
                    || provider.getMaxChunkIndex(cacheId) == 0;
            boolean isEndChunk = chunk.getFinishReason() != null;
            if (isFirstChunk || isEndChunk) {
                addCacheLog(LogLevel.INFO, OPERATION_CACHE_APPEND, cacheId, null, null);
            }
        }
    }

    /**
     * 删除缓存。
     *
     * @param cacheId 缓存 ID
     */
    public void removeCache(String cacheId) {
        addCacheLog(LogLevel.INFO, OPERATION_CACHE_REMOVE, cacheId, null, null);
        provider.removeCache(cacheId);
    }

    /**
     * 从缓存读取从 startIndex 开始的聊天块并转为 SSE 流返回。
     * 缓存不存在、缓存无数据（maxIndex < 0）或 startIndex 大于最大序号时抛出 {@link BusinessException}。
     * 通过统一的轮询循环驱动：初始数据与新增块均按 lastIndex+1 起拉取并检查 finishReason，
     * 直至遇到 finishReason 非 null 的块或超时（生成 finishReason=ERROR 结束块）。
     *
     * @param cacheId    缓存 ID
     * @param startIndex 起始序号
     * @return SSE 聊天块流
     * @throws BusinessException 缓存不存在、缓存无数据或起始序号越界
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

        addCacheLog(LogLevel.INFO, OPERATION_CACHE_STREAM, cacheId, null, null);

        AtomicReference<Integer> lastIndex = new AtomicReference<>(startIndex - 1);
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicLong lastDataTick = new AtomicLong(System.currentTimeMillis());

        return Flux.interval(POLL_INTERVAL)
                .takeUntil(tick -> finished.get())
                .concatMap(tick -> {
                    if (finished.get()) {
                        return Flux.empty();
                    }
                    int from = lastIndex.get() + 1;
                    int currentMax = provider.getMaxChunkIndex(cacheId);
                    if (currentMax >= from) {
                        List<ChatChunk> newChunks = provider.getChunks(cacheId, from, currentMax);
                        lastIndex.set(currentMax);
                        lastDataTick.set(System.currentTimeMillis());
                        for (ChatChunk chunk : newChunks) {
                            if (chunk.getFinishReason() != null) {
                                finished.set(true);
                                break;
                            }
                        }
                        return Flux.fromIterable(newChunks);
                    }
                    if (System.currentTimeMillis() - lastDataTick.get() >= pollTimeoutMs) {
                        log.warn("轮询超时无新数据, cacheId={}, lastIndex={}", cacheId, lastIndex.get());
                        ChatChunk errorChunk = ChatChunk.builder()
                                .index(lastIndex.get() + 1)
                                .finishReason(FinishReason.ERROR)
                                .build();
                        finished.set(true);
                        return Flux.just(errorChunk);
                    }
                    return Flux.empty();
                })
                .map(chunk -> ServerSentEvent.<ChatChunk>builder().data(chunk).build())
                .doFinally(signalType -> addCacheLog(LogLevel.INFO, OPERATION_CACHE_STREAM, cacheId, null, null));
    }

    /**
     * 记录一条智能体日志，agentLog 为 null 时静默跳过，实现抛异常不影响主流程。
     */
    private void addLog(LogData logData) {
        if (agentLog != null) {
            try {
                agentLog.addLog(logData);
            } catch (Exception e) {
                log.warn("记录智能体日志失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 构建并记录聊天数据缓存日志。仅持有 cacheId 时通过 provider 解析会话/对话 ID。
     */
    private void addCacheLog(LogLevel logLevel, String operation, String cacheId, String sessionId, String conversationId) {
        if (agentLog == null) {
            return;
        }
        if (cacheId != null && (sessionId == null || conversationId == null)) {
            try {
                CacheSessionInfo info = provider.getCacheSessionInfo(cacheId);
                if (info != null) {
                    if (sessionId == null) {
                        sessionId = info.sessionId();
                    }
                    if (conversationId == null) {
                        conversationId = info.conversationId();
                    }
                }
            } catch (Exception e) {
                log.warn("根据缓存 ID 获取会话/对话信息失败: {}", e.getMessage(), e);
            }
        }
        addLog(ChatCacheLogData.builder()
                .logLevel(logLevel)
                .operation(operation)
                .sessionId(sessionId)
                .conversationId(conversationId)
                .cacheId(cacheId)
                .build());
    }
}
