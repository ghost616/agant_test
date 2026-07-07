package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.ToolCallDelta;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;


@Slf4j
@Component
@RequiredArgsConstructor
public class SpawnSubAgentSystemTool implements SystemTool {

    private static final int MAX_ITERATIONS = 10;

    private final ModelConfigMapper modelConfigMapper;
    private final ModelInvokerManager modelInvokerManager;
    private final ObjectMapper objectMapper;
    private final ToolManager toolManager;

    private static class ToolAccumulator {
        final StringBuilder id = new StringBuilder();
        final StringBuilder name = new StringBuilder();
        final StringBuilder arguments = new StringBuilder();
    }

    @Override
    public String getToolName() {
        return "spawn_sub_agent";
    }

    @Override
    public String getDescription() {
        return "启动子智能体执行任务";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"agentName\":{\"type\":\"string\",\"description\":\"子智能体名称\"},\"task\":{\"type\":\"string\",\"description\":\"子智能体执行的任务描述\"},\"modelId\":{\"type\":\"string\",\"description\":\"模型 ID（可选）\"}},\"required\":[\"agentName\",\"task\"]}";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments);
            String agentName = root.get("agentName").asText();
            String task = root.get("task").asText();
            JsonNode modelIdNode = root.get("modelId");

            Long modelId;
            if (modelIdNode != null && !modelIdNode.isNull() && !modelIdNode.asText().isBlank()) {
                modelId = modelIdNode.asLong();
            } else {
                modelId = ctx.getModelId();
            }

            ModelConfig modelConfig = modelConfigMapper.selectById(modelId);
            if (modelConfig == null) {
                return "{\"error\":\"model not found: " + modelId + "\"}";
            }

            ModelConfigData configData = new ModelConfigData(modelConfig.getId(), modelConfig.getApiKey(), modelConfig.getBaseUrl(), modelConfig.getModelName(), modelConfig.getTemperature(), modelConfig.getMaxTokens(), modelConfig.getPlatformType().name());
            ModelInvoker invoker = modelInvokerManager.getInvoker(configData);

            List<ToolDefinition> toolDefs = new ArrayList<>();
            for (ToolConfigDTO t : ctx.getTools()) {
                if (!t.getName().startsWith("_sys_")) {
                    toolDefs.add(invoker.toToolDefinition(t));
                }
            }

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role("system")
                    .content("你是一个子智能体，名为" + agentName + "。请完成以下任务。")
                    .build());
            messages.add(Message.builder()
                    .role("user")
                    .content(task)
                    .build());

            for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
                log.info("子智能体迭代 {}/{}，当前消息列表长度 {}", iter + 1, MAX_ITERATIONS, messages.size());

                ChatRequest chatRequest = ChatRequest.builder()
                        .messages(messages)
                        .tools(toolDefs.isEmpty() ? null : toolDefs)
                        .build();

                log.info("子智能体 LLM 调用开始，消息数 {}，工具数 {}", messages.size(), toolDefs.size());

                List<ChatChunk> chunks = invoker.invokeStream(chatRequest).collectList().block();
                StringBuilder contentBuilder = new StringBuilder();
                StringBuilder reasoningBuilder = new StringBuilder();
                Map<String, ToolAccumulator> toolCallBuffers = new LinkedHashMap<>();

                log.info("子智能体 LLM 调用完成，收到 {} chunks", chunks != null ? chunks.size() : 0);

                if (chunks != null) {
                    for (ChatChunk chunk : chunks) {
                        if (chunk.getDelta() != null) {
                            contentBuilder.append(chunk.getDelta());
                        }
                        if (chunk.getReasoning() != null) {
                            reasoningBuilder.append(chunk.getReasoning());
                        }
                        List<ToolCallDelta> tcs = chunk.getToolCalls();
                        if (tcs != null) {
                            for (ToolCallDelta tc : tcs) {
                                String key;
                                if (tc.getIndex() != null) {
                                    key = String.valueOf(tc.getIndex());
                                } else if (tc.getId() != null) {
                                    key = tc.getId();
                                } else {
                                    continue;
                                }
                                ToolAccumulator acc = toolCallBuffers.get(key);
                                if (acc == null || acc.id.length() == 0) {
                                    if (acc == null) {
                                        acc = new ToolAccumulator();
                                        toolCallBuffers.put(key, acc);
                                    }
                                    if (tc.getId() != null) {
                                        acc.id.append(tc.getId());
                                    }
                                    if (tc.getName() != null) {
                                        acc.name.append(tc.getName());
                                    }
                                }
                                if (tc.getArguments() != null) {
                                    acc.arguments.append(tc.getArguments());
                                }
                            }
                        }
                    }
                }

                log.info("子智能体 LLM 调用完成，解析出 {} 个 toolCall", toolCallBuffers.size());

                if (toolCallBuffers.isEmpty()) {
                    return "{\"content\":" + objectMapper.writeValueAsString(contentBuilder.toString()) + ",\"status\":\"ok\"}";
                }

                String content = contentBuilder.toString();
                List<ToolCall> toolCalls = new ArrayList<>();
                for (Map.Entry<String, ToolAccumulator> entry : toolCallBuffers.entrySet()) {
                    ToolAccumulator acc = entry.getValue();
                    String callId = acc.id.length() > 0 ? acc.id.toString() : entry.getKey();
                    toolCalls.add(ToolCall.builder()
                            .id(callId)
                            .name(acc.name.toString())
                            .arguments(acc.arguments.toString())
                            .build());
                }

                messages.add(Message.builder()
                        .role("assistant")
                        .content(content)
                        .reasoning(reasoningBuilder.toString())
                        .toolCalls(toolCalls)
                        .build());

                for (ToolCall tc : toolCalls) {
                    log.info("子智能体执行工具：{}，参数长度 {}", tc.getName(), tc.getArguments() != null ? tc.getArguments().length() : 0);
                    ToolInvoker toolInvoker = toolManager.getInvoker(ctx.getSessionId(), tc.getName());
                    String result;
                    if (toolInvoker == null) {
                        result = objectMapper.writeValueAsString(Map.of("error", "tool not found: " + tc.getName()));
                    } else {
                        try {
                            result = toolInvoker.execute(ctx, tc.getArguments());
                            log.info("子智能体工具 {} 执行完成，结果长度 {}", tc.getName(), result != null ? result.length() : 0);
                        } catch (Exception e) {
                            log.error("子智能体迭代 {} 工具 {} 执行失败: {}", iter + 1, tc.getName(), e.getMessage(), e);
                            result = objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
                        }
                    }
                    messages.add(Message.builder()
                            .role("tool")
                            .toolCallId(tc.getId())
                            .content(result)
                            .build());
                }
            }

            return "{\"content\":\"子智能体达到最大迭代次数\",\"status\":\"max_iterations\"}";
        } catch (Exception e) {
            log.error("spawn_sub_agent 执行失败", e);
            try {
                return objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (Exception inner) {
                return "{\"error\":\"" + inner.getMessage() + "\"}";
            }
        }
    }
}
