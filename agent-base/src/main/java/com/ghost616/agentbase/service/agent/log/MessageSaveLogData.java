package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.dto.model.UsageInfo;
import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体消息保存日志数据，记录消息持久化的关键信息。
 */
@Getter
@SuperBuilder
public class MessageSaveLogData extends SessionLogData {

    /** 消息角色 */
    private final String role;

    /** 保存后的消息 ID */
    private final String messageId;

    /** 消息内容 */
    private final String content;

    /** 推理内容 */
    private final String reasoning;

    /** 工具调用信息 */
    private final ToolInfo toolInfo;

    /** 工具结果 */
    private final String toolResult;

    /** 工具调用列表 */
    private final List<MessageDataProvider.ToolCallData> toolCalls;

    /** Token 用量 */
    private final UsageInfo usage;

    /** 网络搜索调用列表 */
    private final List<MessageDataProvider.WebSearchCallData> webSearchCall;

    /** 自定义工具调用列表 */
    private final List<MessageDataProvider.CustomToolCallData> customToolCall;

    @Override
    public LogType logType() {
        return LogType.MESSAGE_SAVE;
    }
}
