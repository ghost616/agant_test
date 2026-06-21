package com.ghost616.platform.service.hook;

import com.ghost616.platform.dto.model.ChatChunk;
import com.ghost616.platform.enums.HookPhase;
import com.ghost616.platform.service.agent.AgentExecutionContext;

/**
 * 系统 HOOK 接口，扩展 HookInvoker，新增执行顺序控制。
 *
 * @author ghost616
 */
public interface SystemHook extends HookInvoker {

    /**
     * 获取该 HOOK 的执行顺序索引，数值越小越早执行。
     *
     * @return 执行顺序索引，默认 0
     */
    default int getIndex() {
        return 0;
    }
}
