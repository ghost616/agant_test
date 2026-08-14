package com.ghost616.agentinteg.memory;

import com.ghost616.agentinteg.knowledge.SearchType;

import java.util.List;

/**
 * AI 记忆查询 Provider 接口，由外部模块提供记忆的查询能力。
 */
public interface MemoryQueryProvider {

    /**
     * 查询会话的 AI 记忆。
     *
     * @param sessionId  会话 ID
     * @param searchType 搜索类型
     * @param memoryType 记忆类型（GROUP=分类/DAILY=按天，null 或空表示所有记忆）
     * @param startTime  起始时间（毫秒时间戳，可为 null）
     * @param endTime    结束时间（毫秒时间戳，可为 null）
     * @param query      查询关键字
     * @return 记忆结果列表
     */
    List<MemoryResult> getMemories(String sessionId, SearchType searchType, String memoryType,
                                   Long startTime, Long endTime, String query);

    /**
     * 按序号区间列表批量获取消息序号并按角色分类。
     *
     * @param sessionId 会话 ID
     * @param ranges    序号区间列表
     * @return 消息序号按角色分类结果（合并去重后的三个 role 序号列表）
     */
    MessageSeqByRole getMessageSeqsByRole(String sessionId, List<SeqRange> ranges);
}
