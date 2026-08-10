package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ChatChunk;

import java.util.List;

/**
 * 聊天数据缓存提供者接口，定义流式聊天片段的缓存创建、查询、读取与删除契约。
 * 供 ChatDataCacheManager 与具体数据访问层（如平台应用的默认实现）解耦使用。
 */
public interface ChatDataCacheProvider {

    /**
     * 为指定会话与对话创建缓存，返回缓存 ID。
     * 当相同会话与对话的缓存已存在时返回 null。
     *
     * @param sessionId      会话 ID
     * @param conversationId 对话 ID
     * @return 缓存 ID，已存在时返回 null
     */
    String createCache(String sessionId, String conversationId);

    /**
     * 判断缓存是否存在。
     *
     * @param cacheId 缓存 ID
     * @return 存在返回 true，否则返回 false
     */
    boolean cacheExists(String cacheId);

    /**
     * 按会话与对话判断缓存是否存在。
     *
     * @param sessionId      会话 ID
     * @param conversationId 对话 ID
     * @return 存在返回 true，否则返回 false
     */
    boolean cacheExists(String sessionId, String conversationId);

    /**
     * 判断缓存是否已结束（数据写入完成）。
     *
     * @param cacheId 缓存 ID
     * @return 已结束返回 true，否则返回 false
     */
    boolean isCacheDone(String cacheId);

    /**
     * 按会话与对话返回缓存 ID。
     *
     * @param sessionId      会话 ID
     * @param conversationId 对话 ID
     * @return 缓存 ID，不存在时返回 null
     */
    String getCacheId(String sessionId, String conversationId);

    /**
     * 按缓存 ID 返回缓存所属的会话与对话信息。
     *
     * @param cacheId 缓存 ID
     * @return 缓存会话信息（sessionId/conversationId），缓存不存在时返回 null
     */
    CacheSessionInfo getCacheSessionInfo(String cacheId);

    /**
     * 获取缓存中最大块序号，无数据时返回 -1。
     *
     * @param cacheId 缓存 ID
     * @return 最大块序号
     */
    int getMaxChunkIndex(String cacheId);

    /**
     * 向缓存追加一个聊天块。
     *
     * @param cacheId 缓存 ID
     * @param chunk   聊天块
     */
    void appendChunk(String cacheId, ChatChunk chunk);

    /**
     * 删除缓存。
     *
     * @param cacheId 缓存 ID
     */
    void removeCache(String cacheId);

    /**
     * 读取缓存中指定序号范围内的聊天块（含两端）。
     *
     * @param cacheId    缓存 ID
     * @param startIndex 起始序号（含）
     * @param endIndex   结束序号（含）
     * @return 聊天块列表
     */
    List<ChatChunk> getChunks(String cacheId, int startIndex, int endIndex);
}
