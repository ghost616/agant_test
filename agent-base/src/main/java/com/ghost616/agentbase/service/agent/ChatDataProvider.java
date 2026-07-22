package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;

import java.util.List;

public interface ChatDataProvider {

    ModelConfigData getModelConfig(Long modelId);

    void updateSessionModelId(Long sessionId, Long modelId);

    List<HookInvoker> getHooks();

    List<HookInvoker> getHooks(Long sessionId);
}
