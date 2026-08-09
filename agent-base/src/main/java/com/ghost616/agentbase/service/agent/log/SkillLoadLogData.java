package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 智能体技能加载日志数据，记录当前会话已加载的技能信息。
 */
@Getter
@SuperBuilder
public class SkillLoadLogData extends ContextLogData {

    /** 已加载技能名称列表 */
    private final List<String> skillNames;

    /** 已加载技能数量 */
    private final int skillCount;

    @Override
    public LogType logType() {
        return LogType.SKILL_LOAD;
    }
}
