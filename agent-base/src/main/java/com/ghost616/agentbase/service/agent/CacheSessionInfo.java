package com.ghost616.agentbase.service.agent;

/**
 * 缓存会话信息，承载缓存所属的会话 ID 与对话 ID。
 *
 * @param sessionId      会话 ID
 * @param conversationId 对话 ID
 */
public record CacheSessionInfo(String sessionId, String conversationId) {
}
