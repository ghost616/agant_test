package com.ghost616.agentinteg.history;

import java.util.List;

/**
 * 历史消息项数据类，包含角色、内容、推理内容、工具调用列表与工具结果。
 */
public record HistoryMessageItem(String role, String content, String reasoning,
                                 List<HistoryToolCallItem> toolCalls,
                                 HistoryToolResultItem toolResult) {

    /**
     * 工具调用信息，包含调用 ID、工具名与参数 JSON。
     */
    public record HistoryToolCallItem(String toolCallId, String toolCallName, String toolCallArguments) {
    }

    /**
     * 工具结果信息，包含调用 ID 与工具名；结果内容即消息内容（content）。
     */
    public record HistoryToolResultItem(String toolCallId, String toolCallName) {
    }
}
