package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * 智能体请求路由日志数据，记录请求分发到的模型请求类型。
 */
@Getter
@SuperBuilder
public class RouteLogData extends ContextLogData {

    /** 模型请求类型 */
    private final String requestType;

    @Override
    public LogType logType() {
        return LogType.ROUTE;
    }
}
