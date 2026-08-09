package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体子会话创建日志数据，记录子会话创建的关键信息。
 */
@Getter
@SuperBuilder
public class ChildSessionLogData extends SessionLogData {

    /** 子会话 ID */
    private final String childSessionId;

    /** 子会话名称 */
    private final String sessionName;

    /** 子会话描述 */
    private final String description;

    /** 子会话模型 ID */
    private final String modelId;

    /** 子会话可用工具 ID 列表 */
    private final List<String> toolIds;

    /** 子会话可用技能 ID 列表 */
    private final List<String> skillIds;

    /** 子会话提示词 */
    private final String prompt;

    @Override
    public LogType logType() {
        return LogType.CHILD_SESSION;
    }
}
