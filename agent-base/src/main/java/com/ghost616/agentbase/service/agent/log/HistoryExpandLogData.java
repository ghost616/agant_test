package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体历史折叠日志数据，记录历史消息组折叠与展开情况。
 */
@Getter
@SuperBuilder
public class HistoryExpandLogData extends ContextLogData {

    /** 折叠的消息组数量 */
    private final int foldedCount;

    /** 展开的历史消息组锚点消息内容 */
    private final List<String> expandedMessages;

    @Override
    public LogType logType() {
        return LogType.HISTORY_EXPAND;
    }
}
