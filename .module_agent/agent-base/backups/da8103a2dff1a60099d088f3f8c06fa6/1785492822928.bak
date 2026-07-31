package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.invoker.HookData;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.invoker.LoadSkillsSystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceResponsesTest {

    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ChatDataProvider chatDataProvider;
    @Mock
    private SystemToolManager systemToolManager;
    @Mock
    private HookManager hookManager;
    @Mock
    private ModelInvoker modelInvoker;
    @Mock
    private SessionManager.MessageSaveBuilder msgBuilder;

    private AgentComponentRegistry registry;
    private ChatService chatService;

    private final String sessionId = "1";

    @BeforeEach
    void setUp() {
        lenient().when(msgBuilder.sessionId(any())).thenReturn(msgBuilder);
        lenient().when(msgBuilder.role(any())).thenReturn(msgBuilder);
        lenient().when(msgBuilder.content(any())).thenReturn(msgBuilder);
        lenient().when(sessionManager.messageSave()).thenReturn(msgBuilder);

        registry = new AgentComponentRegistry();
        registry.setAgentContextManager(agentContextManager);
        registry.setSessionManager(sessionManager);
        registry.setModelInvokerManager(modelInvokerManager);
        registry.setSystemToolManager(systemToolManager);
        registry.setChatDataProvider(chatDataProvider);
        registry.setHookManager(hookManager);
        chatService = new ChatService(registry);
    }

    private static class TestHarness {
        final AgentExecutionContext context;
        final AgentExecutionContext.AgentContextMutator mutator;

        TestHarness(String systemPrompt, List<ToolConfigDTO> tools,
                    List<SkillConfigDTO> skills, Map<String, String> sessionVariables,
                    List<AgentExecutionContext.HistoryEntry> history) {
            this.mutator = new AgentExecutionContext.AgentContextMutator();
            this.context = new AgentExecutionContext(
                    "1", "1", systemPrompt, "1", null,
                    history != null ? new ArrayList<>(history) : new ArrayList<>(),
                    tools != null ? new ArrayList<>(tools) : new ArrayList<>(),
                    skills, mutator,
                    sessionVariables != null ? sessionVariables : new HashMap<>(),
                    new HashMap<>(), null, "", null);
        }
    }

    private com.ghost616.agentbase.dto.model.ChatRequest executeChat(
            ChatRequest apiRequest, TestHarness harness, String requestType, Flux<ChatChunk> stream) {
        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(
                new AgentContextManager.AgentSessionContext(
                        harness.context, harness.mutator, new AtomicBoolean(false)));

        lenient().when(chatDataProvider.getModelConfig(any())).thenReturn(
                new ModelConfigData("1", "key", "url", "model", 0.7, 4096, "openai", requestType));
        lenient().when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        lenient().when(modelInvoker.invokeStream(any())).thenReturn(stream);
        lenient().when(modelInvoker.toToolDefinition(any())).thenReturn(
                ToolDefinition.builder().name("ctx_tool").build());

        chatService.chat(apiRequest).subscribe();

        ArgumentCaptor<com.ghost616.agentbase.dto.model.ChatRequest> captor =
                ArgumentCaptor.forClass(com.ghost616.agentbase.dto.model.ChatRequest.class);
        verify(modelInvoker).invokeStream(captor.capture());
        return captor.getValue();
    }

    private List<Message> getSystemMessages(com.ghost616.agentbase.dto.model.ChatRequest captured) {
        return captured.getMessages().stream()
                .filter(m -> "system".equals(m.getRole()))
                .toList();
    }

    @Test
    @DisplayName("requestType=responses 时，input 不应包含 system 角色消息，仅含 user/assistant")
    void responses_input应排除system角色消息() {
        List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
        history.add(new AgentExecutionContext.HistoryEntry(
                "system", "历史遗留系统消息", null, null, 1, java.time.LocalDateTime.now(),
                List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "你好", null, null, 2, java.time.LocalDateTime.now(),
                List.of(), null));

        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, history);
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        assertTrue(getSystemMessages(captured).isEmpty(), "input 中不应包含 system 角色消息");
        assertTrue(captured.getMessages().stream().anyMatch(m -> "user".equals(m.getRole())),
                "input 应包含 user 消息");
    }

    @Test
    @DisplayName("requestType=responses 时，instructions 应包含 systemPrompt 与已加载技能提示词")
    void responses_instructions应包含systemPrompt与已加载技能提示词() {
        SkillConfigDTO loadedSkill = SkillConfigDTO.builder()
                .name("loaded_skill")
                .sessionAuth(null)
                .prompt("SKILL_PROMPT_TEXT")
                .build();

        Map<String, String> sessionVars = new HashMap<>();
        sessionVars.put(LoadSkillsSystemTool.SESSION_KEY, "[\"loaded_skill\"]");

        TestHarness harness = new TestHarness("SYSTEM_PROMPT_TEXT", List.of(),
                List.of(loadedSkill), sessionVars, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(
                List.of(ToolDefinition.builder().name(LoadSkillsSystemTool.FULL_TOOL_NAME).build()));

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        assertNotNull(captured.getInstructions());
        assertTrue(captured.getInstructions().contains("SYSTEM_PROMPT_TEXT"),
                "instructions 应包含 systemPrompt");
        assertTrue(captured.getInstructions().contains("SKILL_PROMPT_TEXT"),
                "instructions 应包含已加载技能提示词");
    }

    @Test
    @DisplayName("requestType=responses 时，instructions 应包含可用技能列表")
    void responses_instructions应包含可用技能列表() {
        SkillConfigDTO availableSkill = SkillConfigDTO.builder()
                .name("available_skill")
                .sessionAuth(null)
                .description("desc")
                .build();

        TestHarness harness = new TestHarness("sys_prompt", List.of(),
                List.of(availableSkill), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(
                List.of(ToolDefinition.builder().name(LoadSkillsSystemTool.FULL_TOOL_NAME).build()));

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        assertTrue(captured.getInstructions().contains("可用的技能"),
                "instructions 应包含可用技能列表提示");
        assertTrue(captured.getInstructions().contains("available_skill"));
    }

    @Test
    @DisplayName("requestType=responses 时，previousResponseId 应从 API 请求透传到模型请求")
    void responses_previousResponseId透传() {
        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder()
                .sessionId(sessionId)
                .content("hello")
                .previousResponseId("resp_123")
                .build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        assertEquals("resp_123", captured.getPreviousResponseId());
    }

    @Test
    @DisplayName("requestType=responses 时，工具列表合并上下文工具与系统工具")
    void responses_工具列表合并上下文与系统工具() {
        ToolConfigDTO ctxTool = ToolConfigDTO.builder().name("ctx_tool").build();
        TestHarness harness = new TestHarness("sys_prompt", List.of(ctxTool), List.of(), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(
                List.of(ToolDefinition.builder().name("sys_tool_a").build(),
                        ToolDefinition.builder().name("sys_tool_b").build()));

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        List<String> toolNames = captured.getTools() != null
                ? captured.getTools().stream().map(ToolDefinition::getName).toList() : List.of();
        assertTrue(toolNames.contains("ctx_tool"));
        assertTrue(toolNames.contains("sys_tool_a"));
        assertTrue(toolNames.contains("sys_tool_b"));
    }

    @Test
    @DisplayName("requestType=responses 时，SSE 流 BEFORE_MESSAGE_SEND 与 AFTER_MESSAGE_RECEIVE 钩子正常触发")
    void responses_SSE钩子正常触发() {
        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatChunk chunk = ChatChunk.builder().delta("hi").finishReason("stop").build();

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        executeChat(apiRequest, harness, "responses", Flux.just(chunk));

        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.BEFORE_MESSAGE_SEND,
                harness.context, new HookData(chunk));
        verify(hookManager).triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, harness.context, new HookData(chunk));

        ChatChunk completeChunk = ChatChunk.builder().hasToolCalls(false).build();
        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.AFTER_MESSAGE_RECEIVE,
                harness.context, new HookData(completeChunk));
        verify(hookManager).triggerHooks(HookPhase.AFTER_MESSAGE_RECEIVE, harness.context,
                new HookData(completeChunk));
    }

    @Test
    @DisplayName("requestType=responses 时，有状态分支 input 仅从最后一个 user 条目到末尾")
    void responses_有状态input仅从最后一个user到末尾() {
        List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q1", null, null, 1, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "a1", null, null, 2, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q2", null, null, 3, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "a2", null, null, 4, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "tool", "r2", null, "tc1", 5, java.time.LocalDateTime.now(), List.of(), null));

        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, history);
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder()
                .sessionId(sessionId)
                .content(ChatService.TOOL_CONTINUE_MARKER)
                .build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        List<String> roles = captured.getMessages().stream().map(Message::getRole).toList();
        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        assertEquals(List.of("q2", "a2", "r2"), contents,
                "input 应从最后一个 user(q2) 到末尾，排除此前 q1/a1");
        assertFalse(roles.contains("system"));
    }

    @Test
    @DisplayName("requestType=responses 时，会话 lastResponseId 优先于 API 请求 previousResponseId")
    void responses_lastResponseId优先于API请求() {
        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, List.of());
        harness.context.setLastResponseId("ctx_resp");
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder()
                .sessionId(sessionId)
                .content("hello")
                .previousResponseId("api_resp")
                .build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses", Flux.empty());

        assertEquals("ctx_resp", captured.getPreviousResponseId(),
                "lastResponseId 非空时应优先使用会话上下文值");
    }

    @Test
    @DisplayName("requestType=responses 时，流式 chunk.responseId 应写入会话 lastResponseId")
    void responses_responseId捕获写入上下文() {
        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatChunk chunk = ChatChunk.builder().delta("hi").responseId("r1").build();

        ChatRequest apiRequest = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        executeChat(apiRequest, harness, "responses", Flux.just(chunk));

        assertEquals("r1", harness.context.getLastResponseId(),
                "流式过程中应捕获 responseId 写入会话上下文");
    }

    @Test
    @DisplayName("requestType=responses_stateless 时，input 为全量历史且 previousResponseId 为空")
    void responsesStateless_全量input无previousResponseId() {
        List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
        history.add(new AgentExecutionContext.HistoryEntry(
                "system", "历史遗留系统消息", null, null, 1, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q1", null, null, 2, java.time.LocalDateTime.now(), List.of(), null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "a1", null, null, 3, java.time.LocalDateTime.now(), List.of(), null));

        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, history);
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder()
                .sessionId(sessionId)
                .content("hello")
                .previousResponseId("api_resp")
                .build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "responses_stateless", Flux.empty());

        assertNull(captured.getPreviousResponseId(),
                "无状态分支不应传 previousResponseId");
        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        assertEquals(List.of("q1", "a1", "hello"), contents,
                "无状态分支 input 应包含全量历史（不含 system），且含本轮 user");
        assertTrue(getSystemMessages(captured).isEmpty(), "input 不应包含 system 角色消息");
    }

    @Test
    @DisplayName("requestType=openai 时，走 chat completions 分支：messages 含 system 消息，无 instructions")
    void openai_走chatCompletions含system消息() {
        TestHarness harness = new TestHarness("sys_prompt", List.of(), List.of(), null, List.of());
        when(systemToolManager.getToolDefinitions()).thenReturn(List.of());

        ChatRequest apiRequest = ChatRequest.builder()
                .sessionId(sessionId)
                .content("hello")
                .previousResponseId("resp_123")
                .build();
        com.ghost616.agentbase.dto.model.ChatRequest captured =
                executeChat(apiRequest, harness, "openai", Flux.empty());

        assertEquals(1, getSystemMessages(captured).size(), "chat completions 分支应含 system 消息");
        assertNull(captured.getInstructions());
        assertNull(captured.getPreviousResponseId());
    }
}
