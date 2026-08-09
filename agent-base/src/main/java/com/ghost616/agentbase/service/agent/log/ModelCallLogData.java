package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体模型调用日志数据，记录模型请求的消息与工具规模。
 */
@Getter
@SuperBuilder
public class ModelCallLogData extends ContextLogData {

    /** 请求消息数量 */
    private final int messageCount;

    /** 工具数量 */
    private final int toolCount;

    /** 发给模型的工具名称列表 */
    private final List<String> toolNames;

    /** 是否启用思考模式 */
    private final Boolean thinking;

    @Override
    public LogType logType() {
        return LogType.MODEL_CALL;
    }
}
