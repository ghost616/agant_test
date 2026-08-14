package com.ghost616.agentinteg.history;

import java.util.List;

/**
 * 历史消息查询 Provider 接口，由外部模块提供历史消息的查询能力。
 */
public interface HistoryMessageQueryProvider {

    /**
     * 按消息序号列表查询历史消息。
     *
     * @param sessionId        会话 ID
     * @param seqs             消息序号列表
     * @param includeReasoning 是否包含推理内容
     * @return 历史消息列表
     */
    List<HistoryMessageItem> getMessagesBySeqs(String sessionId, List<Integer> seqs, boolean includeReasoning);
}
