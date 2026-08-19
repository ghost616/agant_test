package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;

import java.util.List;

public interface ChatDataProvider {

    ModelConfigData getModelConfig(String modelId);

    void updateSessionModelId(String sessionId, String modelId);

    List<HookInvoker> getHooks();

    List<HookInvoker> getHooks(String sessionId);

    /**
     * 获取会话级前置系统提示词，注入到主 system prompt 之后。
     * 返回 null 或空白字符串时跳过注入。
     *
     * @param sessionId 会话 ID
     * @return 前置系统提示词，无则返回 null
     */
    String getPreSystemPrompt(String sessionId);

    /**
     * 获取会话级后置系统提示词，注入到最后一个 user 消息之前（无 user 消息时追加到末尾）。
     * 返回 null 或空白字符串时跳过注入。
     *
     * @param sessionId 会话 ID
     * @return 后置系统提示词，无则返回 null
     */
    String getPostSystemPrompt(String sessionId);
}
