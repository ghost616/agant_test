package com.ghost616.agentbase.service.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ghost616.agentbase.util.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.agentbase.service.agent.invoker.SystemHook;
import com.ghost616.agentbase.service.agent.invoker.SystemPostHook;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;

@Slf4j
public class ChatService {

    public static final String TOOL_CONTINUE_MARKER = "[tool_continue]";

    private final AgentContextManager agentContextManager;
    private final SessionManager sessionManager;
    private final ModelInvokerManager modelInvokerManager;
    private final SystemToolManager systemToolManager;
    private final ChatDataProvider chatDataProvider;

    private final Map<HookPhase, List<HookInvoker>> systemHooks = new HashMap<>();
    private final List<HookInvoker> systemPostHooks = new ArrayList<>();
    private final Map<HookPhase, List<HookInvoker>> regularPhaseHooks = new HashMap<>();

    public ChatService(AgentContextManager agentContextManager,
                       SessionManager sessionManager,
                       ModelInvokerManager modelInvokerManager,
                       SystemToolManager systemToolManager,
                       ChatDataProvider chatDataProvider) {
        this.agentContextManager = agentContextManager;
        this.sessionManager = sessionManager;
        this.modelInvokerManager = modelInvokerManager;
        this.systemToolManager = systemToolManager;
        this.chatDataProvider = chatDataProvider;
    }

    public void initHooks() {
        List<HookInvoker> hooks = chatDataProvider.getHooks();
        for (HookInvoker hook : hooks) {
            HookPhase phase = hook.getPhase();
            if (hook instanceof SystemPostHook) {
                systemPostHooks.add(hook);
            } else if (hook instanceof SystemHook) {
                systemHooks.computeIfAbsent(phase, k -> new ArrayList<>()).add(hook);
            } else {
                regularPhaseHooks.computeIfAbsent(phase, k -> new ArrayList<>()).add(hook);
            }
        }
    }

    private void triggerHooks(HookPhase phase, AgentExecutionContext ctx, ChatChunk chunk) {
        List<HookInvoker> regularHooks = regularPhaseHooks.get(phase);
        if (regularHooks != null) {
            regularHooks.forEach(h -> h.execute(ctx, chunk));
        }
        List<HookInvoker> hooks = systemHooks.get(phase);
        if (hooks != null) {
            hooks.stream()
                    .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                    .forEach(h -> h.execute(ctx, chunk));
        }
    }

    private void executePostHooks(AgentExecutionContext ctx, ChatChunk chunk) {
        systemPostHooks.stream()
                .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                .forEach(h -> h.execute(ctx, chunk));
    }

    public Flux<ServerSentEvent<ChatChunk>> chat(ChatRequest request) {
        Long sessionId = request.getSessionId();
        String content = request.getContent();
        Long modelId = request.getModelId();

        AgentContextManager.AgentSessionContext sessionContext =
                agentContextManager.build(sessionId).modelIdOverride(modelId).build();
        AgentExecutionContext context = sessionContext.context();
        AgentExecutionContext.AgentContextMutator contextMutator = sessionContext.mutator();

        boolean isToolContinue = TOOL_CONTINUE_MARKER.equals(content);

        if (!isToolContinue) {
            contextMutator.resetStopped();
            contextMutator.clearConversationVariables();
            sessionManager.save().sessionId(sessionId).role("user").content(content).save();

            AgentExecutionContext.HistoryEntry userEntry = new AgentExecutionContext.HistoryEntry(
                    "user", content, null, null,
                    context.getHistory().size() + 1,
                    LocalDateTime.now(),
                    Collections.emptyList());
            contextMutator.addHistoryEntry(userEntry);
        }

        if (modelId != null && !modelId.equals(context.getModelId())) {
            chatDataProvider.updateSessionModelId(sessionId, modelId);
            contextMutator.setModelId(modelId);
        }

        Long finalModelId = (modelId != null) ? modelId : context.getModelId();
        ModelConfigData configData = chatDataProvider.getModelConfig(finalModelId);
        if (configData == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }

        triggerHooks(HookPhase.SESSION_START, context, null);

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role("system")
                .content(context.getSystemPrompt() != null ? context.getSystemPrompt() : "")
                .build());

        List<SkillConfigDTO> skills = context.getSkills();
        if (skills != null && !skills.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("以下是可用的技能（SKILL）列表（技能本身不是工具，需先加载再使用其关联的工具）：\n");
            for (SkillConfigDTO skill : skills) {
                sb.append("- ").append(skill.getName());
                if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
                    sb.append(": ").append(skill.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n请使用 _sys_load_skills 系统工具加载所需技能。加载后，该技能的关联工具将变为可用，届时再调用具体工具。禁止直接以技能名称作为工具调用。");
            messages.add(Message.builder()
                    .role("system")
                    .content(sb.toString())
                    .build());
        }

        List<SkillConfigDTO> loadedSkills = parseLoadedSkills(context, skills);
        if (!loadedSkills.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("以下技能已加载，请按照其提示词指导执行任务：\n\n");
            for (SkillConfigDTO skill : loadedSkills) {
                sb.append("## ").append(skill.getName()).append("\n");
                if (skill.getPrompt() != null && !skill.getPrompt().isEmpty()) {
                    sb.append(skill.getPrompt()).append("\n\n");
                }
            }
            messages.add(Message.builder()
                    .role("system")
                    .content(sb.toString())
                    .build());
        }

        for (AgentExecutionContext.HistoryEntry entry : context.getHistory()) {
            Message.MessageBuilder builder = Message.builder()
                    .role(entry.role())
                    .content(entry.content());
            if (entry.toolCalls() != null && !entry.toolCalls().isEmpty()) {
                builder.toolCalls(entry.toolCalls());
            }
            if (entry.reasoning() != null && !entry.reasoning().isEmpty()
                    && entry.toolCalls() != null && !entry.toolCalls().isEmpty()) {
                builder.reasoning(entry.reasoning());
            }
            if (entry.toolCallId() != null) {
                builder.toolCallId(entry.toolCallId());
            }
            messages.add(builder.build());
        }

        messages = foldMessageGroups(messages, context);

        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);

        Map<String, ToolDefinition> toolMap = new LinkedHashMap<>();
        for (ToolConfigDTO t : context.getTools()) {
            ToolDefinition def = invoker.toToolDefinition(t);
            toolMap.put(def.getName(), def);
        }
        for (ToolDefinition def : systemToolManager.getToolDefinitions()) {
            toolMap.put(def.getName(), def);
        }
        for (SkillConfigDTO skill : loadedSkills) {
            if (skill.getSkillTools() != null) {
                for (ToolConfigDTO st : skill.getSkillTools()) {
                    ToolDefinition def = invoker.toToolDefinition(st);
                    toolMap.put(def.getName(), def);
                }
            }
        }
        List<ToolDefinition> tools = new ArrayList<>(toolMap.values());

        com.ghost616.agentbase.dto.model.ChatRequest chatRequest =
                com.ghost616.agentbase.dto.model.ChatRequest.builder()
                        .messages(messages)
                        .tools(tools)
                        .thinking(request.getThinking())
                        .build();

        AtomicBoolean hasToolCalls = new AtomicBoolean(false);

        Flux<ChatChunk> stream = invoker.invokeStream(chatRequest);

        return stream
                .takeWhile(chunk -> !context.isStopped())
                .doOnNext(chunk -> {
                    if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                        hasToolCalls.set(true);
                    }
                    if (chunk.getFinishReason() != null) {
                        chunk.setHasToolCalls(hasToolCalls.get());
                    }
                    triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, context, chunk);
                    executePostHooks(context, chunk);
                })
                .map(chunk -> ServerSentEvent.<ChatChunk>builder()
                        .data(chunk)
                        .build())
                .doOnComplete(() -> {
                    ChatChunk completeChunk = ChatChunk.builder()
                            .hasToolCalls(hasToolCalls.get())
                            .build();
                    triggerHooks(HookPhase.AFTER_MESSAGE_RECEIVE, context, completeChunk);
                    executePostHooks(context, completeChunk);
                })
                .doOnCancel(() -> contextMutator.setStopped());
    }

    private List<SkillConfigDTO> parseLoadedSkills(AgentExecutionContext context, List<SkillConfigDTO> skills) {
        String json = context.getSessionVariable("_sys_loading_SKILLS");
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<String> loadedNames;
        try {
            loadedNames = JsonMapper.MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 _sys_loading_SKILLS 失败: {}", json, e);
            return List.of();
        }
        if (loadedNames == null || loadedNames.isEmpty()) {
            return List.of();
        }
        Set<String> nameSet = new HashSet<>(loadedNames);
        List<SkillConfigDTO> result = new ArrayList<>();
        if (skills != null) {
            for (SkillConfigDTO skill : skills) {
                if (nameSet.contains(skill.getName())) {
                    result.add(skill);
                }
            }
        }
        return result;
    }

    private List<Message> foldMessageGroups(List<Message> messages, AgentExecutionContext context) {
        Integer recentCount = context.getRecentMessageCount();
        if (recentCount == null || recentCount <= 0) {
            return messages;
        }

        Set<Integer> expandedIndices = parseExpandedIndices(
                context.getConversationVariable("_sys_his_msgs_index"));

        List<List<Message>> groups = new ArrayList<>();
        int i = 1;
        while (i < messages.size()) {
            int groupStart = i;
            i++;
            while (i < messages.size() && !"user".equals(messages.get(i).getRole())) {
                i++;
            }
            List<Message> group = new ArrayList<>();
            for (int j = groupStart; j < i; j++) {
                group.add(messages.get(j));
            }
            groups.add(group);
        }

        if (groups.size() <= recentCount) {
            return messages;
        }

        int foldEnd = groups.size() - recentCount;

        List<Message> result = new ArrayList<>();
        result.add(messages.get(0));

        for (int g = 0; g < groups.size(); g++) {
            if (g < foldEnd && !expandedIndices.contains(g)) {
                List<Message> group = groups.get(g);
                result.add(group.get(0));
                result.add(Message.builder()
                        .role("assistant")
                        .content("此为历史消息索引为" + g + "，如果想要展开请调用历史消息工具")
                        .build());
            } else {
                result.addAll(groups.get(g));
            }
        }

        return result;
    }

    private Set<Integer> parseExpandedIndices(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Collections.emptySet();
        }
        try {
            List<Integer> list = JsonMapper.MAPPER.readValue(
                    jsonStr, new TypeReference<List<Integer>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            log.warn("解析 _sys_his_msgs_index 失败: {}", jsonStr, e);
            return Collections.emptySet();
        }
    }
}
