package com.ghost616.agentbase.service.model.invoker;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.invoker.ToolDefinitionConverter;
import reactor.core.publisher.Flux;

/**
 * 统一模型调用接口，屏蔽不同 LLM 提供商的差异。
 */
public interface ModelInvoker {

    /**
     * 同步调用模型。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    ChatResponse invoke(ChatRequest request);

    /**
     * 流式调用模型。
     *
     * @param request 对话请求
     * @return 流式对话片段
     */
    Flux<ChatChunk> invokeStream(ChatRequest request);

    /**
     * 连通性验证。
     *
     * @return 验证成功返回 true，否则返回 false
     */
    boolean verify();

    /**
     * 构建仅含名称和描述的最小工具定义，不包含参数 Schema。
     *
     * @param tool 工具配置
     * @return 无 parameters 的 ToolDefinition
     */
    default ToolDefinition createMinimalToolDefinition(ToolConfigDTO tool) {
        return ToolDefinitionConverter.createMinimal(tool);
    }

    /**
     * 将工具配置 DTO 转换为 ToolDefinition，自动补全 JSON Schema 的 type 字段。
     *
     * @param tool 工具配置
     * @return 工具定义
     */
    default ToolDefinition toToolDefinition(ToolConfigDTO tool) {
        return ToolDefinitionConverter.convert(tool);
    }
}
