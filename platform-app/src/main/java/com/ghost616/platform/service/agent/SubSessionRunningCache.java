package com.ghost616.platform.service.agent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 子会话运行缓存，记录正在执行中的 WEBSOCKET 子会话。
 *
 * <p>WEBSOCKET 打开方式的子会话被父会话通过 callback_sub_session 工具调用后开始执行
 * （前端 runChildSessionFlow 流式回复中），若父会话再次复用同名子会话发送消息会重复触发
 * 子会话执行。本组件以子会话 ID 为键记录运行中子会话：执行开始时 {@link #add} 记录，
 * 执行完成（兜底回传判定触发时）由 {@link #remove} 移除；再次发送命中 {@link #contains}
 * 时返回错误 JSON「子会话正在运行，请等候」，阻止重复执行。</p>
 *
 * <p>基于 {@link ConcurrentHashMap} 保证线程安全；容量上限 10000，超限时清空整个缓存
 * （与 {@link SubSessionWebSocketModeResolver} 的缓存策略一致），防止内存泄漏。</p>
 */
@Component
public class SubSessionRunningCache {

    private static final int MAX_CACHE_SIZE = 10000;

    private final ConcurrentHashMap<Long, Boolean> runningSessions = new ConcurrentHashMap<>();

    /**
     * 判断指定子会话是否正在执行中。
     *
     * @param childSessionId 子会话 ID
     * @return true 表示运行中；childSessionId 为 null 时返回 false
     */
    public boolean contains(Long childSessionId) {
        return childSessionId != null && runningSessions.containsKey(childSessionId);
    }

    /**
     * 记录子会话开始执行。
     *
     * @param childSessionId 子会话 ID
     */
    public void add(Long childSessionId) {
        if (childSessionId == null) {
            return;
        }
        if (runningSessions.size() >= MAX_CACHE_SIZE) {
            runningSessions.clear();
        }
        runningSessions.put(childSessionId, Boolean.TRUE);
    }

    /**
     * 移除子会话运行标记（子会话执行完成后调用，允许后续再次发送）。
     *
     * @param childSessionId 子会话 ID
     */
    public void remove(Long childSessionId) {
        if (childSessionId != null) {
            runningSessions.remove(childSessionId);
        }
    }
}
