package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class ChatServiceFilteringTest {

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
    private ModelInvoker modelInvoker;
    @Mock
    private SessionManager.MessageSaveBuilder msgBuilder;

    private AgentComponentRegistry registry;
    private ChatService chatService;

    private final AtomicBoolean toolInvoking = new AtomicBoolean(false);

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
        chatService = new ChatService(registry);
    }

    private static class TestHarness {
        final AgentExecutionContext context;
        final AgentExecutionContext.AgentContextMutator mutator;

        TestHarness(Long sessionId, String systemPrompt, List<ToolConfigDTO> tools,
                    List<SkillConfigDTO> skills, Map<String, String> sessionVariables,
                    Long parentSessionId) {
            this.mutator = new AgentExecutionContext.AgentContextMutator();
            this.context = new AgentExecutionContext(
                    sessionId, 1L, systemPrompt, 1L, null,
                    new ArrayList<>(), tools != null ? new ArrayList<>(tools) : new ArrayList<>(),
                    skills != null ? new ArrayList<>(skills) : null, mutator,
                    sessionVariables != null ? sessionVariables : new HashMap<>(),
                    new HashMap<>(), parentSessionId, new ArrayList<>());
        }
    }

    private void mockChatInfrastructure() {
        lenient().when(chatDataProvider.getModelConfig(any())).thenReturn(
                new ModelConfigData(1L, "key", "url", "model", 0.7, 1000, "test"));
        lenient().when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        lenient().when(modelInvoker.invokeStream(any())).thenReturn(Flux.empty());
        lenient().when(modelInvoker.toToolDefinition(any())).thenReturn(
                com.ghost616.agentbase.dto.model.ToolDefinition.builder().name("test_tool").build());
        lenient().when(systemToolManager.getToolDefinitions()).thenReturn(
                List.of(ToolDefinition.builder().name("_sys_load_skills").build()));
    }

    private com.ghost616.agentbase.dto.model.ChatRequest executeChat(Long sessionId, TestHarness harness) {
        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(
                new AgentContextManager.AgentSessionContext(
                        harness.context, harness.mutator, toolInvoking));

        mockChatInfrastructure();

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .content("hello")
                .build();

        chatService.chat(request).subscribe();

        ArgumentCaptor<com.ghost616.agentbase.dto.model.ChatRequest> captor =
                ArgumentCaptor.forClass(com.ghost616.agentbase.dto.model.ChatRequest.class);
        verify(modelInvoker).invokeStream(captor.capture());
        return captor.getValue();
    }

    private List<com.ghost616.agentbase.dto.model.Message> getSystemMessages(
            com.ghost616.agentbase.dto.model.ChatRequest captured) {
        return captured.getMessages().stream()
                .filter(m -> "system".equals(m.getRole()))
                .toList();
    }

    private String findMessageByContent(List<com.ghost616.agentbase.dto.model.Message> msgs, String keyword) {
        return msgs.stream()
                .map(com.ghost616.agentbase.dto.model.Message::getContent)
                .filter(c -> c.contains(keyword))
                .findFirst()
                .orElse(null);
    }

    @Nested
    @DisplayName("Section 1: 可用技能列表过滤（第160-179行）")
    class SkillsFilteringTest {

        @Test
        @DisplayName("主会话时，sessionAuth == CHILD 的技能应从技能列表消息中过滤")
        void mainSession_shouldFilterChildSkills() {
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_only").sessionAuth(SessionAuthType.CHILD).description("child desc").build();
            SkillConfigDTO parentSkill = SkillConfigDTO.builder()
                    .name("parent_only").sessionAuth(SessionAuthType.PARENT).description("parent desc").build();
            SkillConfigDTO allSkill = SkillConfigDTO.builder()
                    .name("all_skill").sessionAuth(SessionAuthType.ALL).description("all desc").build();
            SkillConfigDTO nullAuthSkill = SkillConfigDTO.builder()
                    .name("null_auth").description("null desc").build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(),
                    List.of(childSkill, parentSkill, allSkill, nullAuthSkill), null, null);
            var captured = executeChat(1L, harness);

            String skillsContent = findMessageByContent(getSystemMessages(captured), "可用的技能");
            assertNotNull(skillsContent, "应有技能列表消息");
            assertTrue(skillsContent.contains("parent_only"), "应包含 PARENT 权限的技能");
            assertTrue(skillsContent.contains("all_skill"), "应包含 ALL 权限的技能");
            assertTrue(skillsContent.contains("null_auth"), "应包含 null 权限的技能");
            assertFalse(skillsContent.contains("child_only"), "不应包含 CHILD 权限的技能");
        }

        @Test
        @DisplayName("主会话时，sessionAuth 为 null/ALL/PARENT 的技能应正常显示")
        void mainSession_shouldIncludeParentAllAndNullSkills() {
            SkillConfigDTO parentSkill = SkillConfigDTO.builder()
                    .name("parent_skill").sessionAuth(SessionAuthType.PARENT).build();
            SkillConfigDTO nullSkill = SkillConfigDTO.builder()
                    .name("null_skill").build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(),
                    List.of(parentSkill, nullSkill), null, null);
            var captured = executeChat(1L, harness);

            String skillsContent = findMessageByContent(getSystemMessages(captured), "可用的技能");
            assertNotNull(skillsContent);
            assertTrue(skillsContent.contains("parent_skill"));
            assertTrue(skillsContent.contains("null_skill"));
        }

        @Test
        @DisplayName("非主会话（子会话）时，不过滤任何技能")
        void childSession_shouldNotFilterSkills() {
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_skill").sessionAuth(SessionAuthType.CHILD).build();
            SkillConfigDTO parentSkill = SkillConfigDTO.builder()
                    .name("parent_skill").sessionAuth(SessionAuthType.PARENT).build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(),
                    List.of(childSkill, parentSkill), null, 99L);
            var captured = executeChat(1L, harness);

            String skillsContent = findMessageByContent(getSystemMessages(captured), "可用的技能");
            assertNotNull(skillsContent);
            assertTrue(skillsContent.contains("child_skill"), "子会话应显示 CHILD 技能");
            assertTrue(skillsContent.contains("parent_skill"), "子会话应显示 PARENT 技能");
        }

        @Test
        @DisplayName("技能列表为空时，不生成可用技能消息")
        void emptySkills_shouldNotAddSkillMessage() {
            var harness = new TestHarness(1L, "sys_prompt", List.of(), List.of(), null, null);
            var captured = executeChat(1L, harness);

            String skillsContent = findMessageByContent(getSystemMessages(captured), "可用的技能");
            assertNull(skillsContent, "不应有技能列表消息");
        }

        @Test
        @DisplayName("skills 为 null 时，不生成可用技能消息")
        void nullSkills_shouldNotAddSkillMessage() {
            var harness = new TestHarness(1L, "sys_prompt", List.of(), null, null, null);
            var captured = executeChat(1L, harness);

            String skillsContent = findMessageByContent(getSystemMessages(captured), "可用的技能");
            assertNull(skillsContent, "不应有技能列表消息");
        }
    }

    @Nested
    @DisplayName("Section 2: 已加载技能提示词过滤（第181-198行）")
    class LoadedSkillsPromptFilteringTest {

        private TestHarness createHarnessWithLoadedSkills(List<SkillConfigDTO> skills,
                                                           List<String> loadedSkillNames,
                                                           Long parentSessionId) {
            Map<String, String> sessionVars = new HashMap<>();
            sessionVars.put("_sys_loading_SKILLS",
                    "[\"" + String.join("\",\"", loadedSkillNames) + "\"]");
            return new TestHarness(1L, "sys_prompt", List.of(), skills, sessionVars, parentSessionId);
        }

        @Test
        @DisplayName("主会话时，已加载技能中 sessionAuth == CHILD 的应被跳过")
        void mainSession_shouldSkipChildLoadedSkills() {
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_loaded").sessionAuth(SessionAuthType.CHILD).prompt("child prompt").build();
            SkillConfigDTO allSkill = SkillConfigDTO.builder()
                    .name("all_loaded").sessionAuth(SessionAuthType.ALL).prompt("all prompt").build();

            var harness = createHarnessWithLoadedSkills(
                    List.of(childSkill, allSkill), List.of("child_loaded", "all_loaded"), null);
            var captured = executeChat(1L, harness);

            String loadedContent = findMessageByContent(getSystemMessages(captured), "以下技能已加载");
            assertNotNull(loadedContent);
            assertTrue(loadedContent.contains("all_loaded"), "应包含 ALL 技能");
            assertTrue(loadedContent.contains("all prompt"));
            assertFalse(loadedContent.contains("child_loaded"), "不应包含 CHILD 技能");
            assertFalse(loadedContent.contains("child prompt"));
        }

        @Test
        @DisplayName("主会话时，已加载技能中 sessionAuth 非 CHILD 的其他技能应正常显示")
        void mainSession_shouldIncludeNonChildLoadedSkills() {
            SkillConfigDTO parentSkill = SkillConfigDTO.builder()
                    .name("parent_loaded").sessionAuth(SessionAuthType.PARENT).prompt("parent prompt").build();
            SkillConfigDTO nullSkill = SkillConfigDTO.builder()
                    .name("null_loaded").prompt("null prompt").build();

            var harness = createHarnessWithLoadedSkills(
                    List.of(parentSkill, nullSkill), List.of("parent_loaded", "null_loaded"), null);
            var captured = executeChat(1L, harness);

            String loadedContent = findMessageByContent(getSystemMessages(captured), "以下技能已加载");
            assertNotNull(loadedContent);
            assertTrue(loadedContent.contains("parent_loaded"));
            assertTrue(loadedContent.contains("null_loaded"));
        }

        @Test
        @DisplayName("非主会话时，已加载技能不过滤 CHILD 权限")
        void childSession_shouldNotFilterChildLoadedSkills() {
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_loaded").sessionAuth(SessionAuthType.CHILD).prompt("child prompt").build();

            var harness = createHarnessWithLoadedSkills(
                    List.of(childSkill), List.of("child_loaded"), 99L);
            // 子会话的 getSessionVariable 通过 mutator 回调委托给父会话，需设置回调
            harness.mutator.getSessionVarCallback = key -> {
                if ("_sys_loading_SKILLS".equals(key)) {
                    return "[\"child_loaded\"]";
                }
                return null;
            };
            var captured = executeChat(1L, harness);

            String loadedContent = findMessageByContent(getSystemMessages(captured), "以下技能已加载");
            assertNotNull(loadedContent);
            assertTrue(loadedContent.contains("child_loaded"), "子会话应显示 CHILD 技能");
        }

        @Test
        @DisplayName("没有任何已加载技能时，不生成提示词消息")
        void noLoadedSkills_shouldNotAddPromptMessage() {
            SkillConfigDTO skill = SkillConfigDTO.builder()
                    .name("not_loaded").sessionAuth(SessionAuthType.ALL).prompt("prompt").build();

            Map<String, String> sessionVars = new HashMap<>();
            sessionVars.put("_sys_loading_SKILLS", "[]");
            var harness = new TestHarness(1L, "sys_prompt", List.of(), List.of(skill), sessionVars, null);
            var captured = executeChat(1L, harness);

            String loadedContent = findMessageByContent(getSystemMessages(captured), "以下技能已加载");
            assertNull(loadedContent, "不应有已加载技能提示词消息");
        }

        @Test
        @DisplayName("_sys_loading_SKILLS 为 null 时，不生成提示词消息")
        void nullLoadingSkills_shouldNotAddPromptMessage() {
            SkillConfigDTO skill = SkillConfigDTO.builder()
                    .name("skill_a").sessionAuth(SessionAuthType.ALL).prompt("prompt").build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(), List.of(skill), null, null);
            var captured = executeChat(1L, harness);

            String loadedContent = findMessageByContent(getSystemMessages(captured), "以下技能已加载");
            assertNull(loadedContent, "不应有已加载技能提示词消息");
        }
    }

    @Nested
    @DisplayName("Section 3: 子会话能力提示合并（第200-247行）")
    class ChildSessionCapabilitiesMergeTest {

        @Test
        @DisplayName("只有子会话工具时：只生成工具消息，不含技能消息")
        void onlyTools_shouldGenerateToolsMessage() {
            ToolConfigDTO childTool = ToolConfigDTO.builder()
                    .name("child_tool").sessionAuth(SessionAuthType.CHILD).description("for child").build();
            ToolConfigDTO allTool = ToolConfigDTO.builder()
                    .name("all_tool").sessionAuth(SessionAuthType.ALL).build();
            ToolConfigDTO nullTool = ToolConfigDTO.builder()
                    .name("null_tool").build();

            var harness = new TestHarness(1L, "sys_prompt",
                    List.of(childTool, allTool, nullTool), null, null, null);
            var captured = executeChat(1L, harness);

            String capsContent = findMessageByContent(getSystemMessages(captured), "子会话可");
            assertNotNull(capsContent, "应有子会话能力消息");
            assertTrue(capsContent.contains("子会话可使用以下工具"));
            assertTrue(capsContent.contains("child_tool"));
            assertTrue(capsContent.contains("all_tool"));
            assertTrue(capsContent.contains("null_tool"));
            assertFalse(capsContent.contains("子会话可使用以下技能"));
        }

        @Test
        @DisplayName("只有子会话技能时：只生成技能消息，不含工具消息")
        void onlySkills_shouldGenerateSkillsMessage() {
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_skill").sessionAuth(SessionAuthType.CHILD).description("for child").build();
            SkillConfigDTO allSkill = SkillConfigDTO.builder()
                    .name("all_skill").sessionAuth(SessionAuthType.ALL).build();
            SkillConfigDTO nullSkill = SkillConfigDTO.builder()
                    .name("null_skill").build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(),
                    List.of(childSkill, allSkill, nullSkill), null, null);
            var captured = executeChat(1L, harness);

            String capsContent = findMessageByContent(getSystemMessages(captured), "子会话可");
            assertNotNull(capsContent);
            assertTrue(capsContent.contains("子会话可使用以下技能"));
            assertTrue(capsContent.contains("child_skill"));
            assertTrue(capsContent.contains("all_skill"));
            assertTrue(capsContent.contains("null_skill"));
            assertFalse(capsContent.contains("子会话可使用以下工具"));
        }

        @Test
        @DisplayName("同时存在工具和技能时：合并为单一消息，先工具后技能，中间有空行分隔")
        void bothToolsAndSkills_shouldMergeWithBlankLine() {
            ToolConfigDTO childTool = ToolConfigDTO.builder()
                    .name("child_tool").sessionAuth(SessionAuthType.CHILD).build();
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_skill").sessionAuth(SessionAuthType.CHILD).build();

            var harness = new TestHarness(1L, "sys_prompt",
                    List.of(childTool), List.of(childSkill), null, null);
            var captured = executeChat(1L, harness);

            var sysMsgs = getSystemMessages(captured);
            long capsMsgCount = sysMsgs.stream()
                    .filter(m -> m.getContent().contains("子会话可"))
                    .count();
            assertEquals(1, capsMsgCount, "子会话能力提示应合并为一条消息");

            String capsContent = findMessageByContent(sysMsgs, "子会话可");
            assertNotNull(capsContent);
            assertTrue(capsContent.contains("子会话可使用以下工具"));
            assertTrue(capsContent.contains("子会话可使用以下技能"));

            int toolsIdx = capsContent.indexOf("子会话可使用以下工具");
            int skillsIdx = capsContent.indexOf("子会话可使用以下技能");
            assertTrue(toolsIdx < skillsIdx, "工具应出现在技能之前");

            String between = capsContent.substring(toolsIdx + "子会话可使用以下工具".length(), skillsIdx);
            assertTrue(between.contains("\n\n") || between.trim().isEmpty(),
                    "工具和技能之间应有空行分隔");
        }

        @Test
        @DisplayName("工具和技能均为空时：不生成子会话能力消息")
        void bothEmpty_shouldNotGenerateMessage() {
            var harness = new TestHarness(1L, "sys_prompt", List.of(), List.of(), null, null);
            var captured = executeChat(1L, harness);

            String capsContent = findMessageByContent(getSystemMessages(captured), "子会话可");
            assertNull(capsContent, "不应有子会话能力消息");
        }

        @Test
        @DisplayName("子会话技能取 sessionAuth 为 null/ALL/CHILD 的（不应含 PARENT）")
        void childSessionSkills_shouldIncludeNullAllChild() {
            ToolConfigDTO tool = ToolConfigDTO.builder()
                    .name("child_tool").sessionAuth(SessionAuthType.CHILD).build();
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_skill").sessionAuth(SessionAuthType.CHILD).build();
            SkillConfigDTO allSkill = SkillConfigDTO.builder()
                    .name("all_skill").sessionAuth(SessionAuthType.ALL).build();
            SkillConfigDTO nullSkill = SkillConfigDTO.builder()
                    .name("null_skill").build();
            SkillConfigDTO parentSkill = SkillConfigDTO.builder()
                    .name("parent_skill").sessionAuth(SessionAuthType.PARENT).build();

            var harness = new TestHarness(1L, "sys_prompt", List.of(tool),
                    List.of(childSkill, allSkill, nullSkill, parentSkill), null, null);
            var captured = executeChat(1L, harness);

            String capsContent = findMessageByContent(getSystemMessages(captured), "子会话可");
            assertNotNull(capsContent);
            assertTrue(capsContent.contains("child_skill"), "子会话应包含 CHILD 技能");
            assertTrue(capsContent.contains("all_skill"), "子会话应包含 ALL 技能");
            assertTrue(capsContent.contains("null_skill"), "子会话应包含 null 权限技能");
            assertFalse(capsContent.contains("parent_skill"), "子会话不应包含 PARENT 技能");
        }

        @Test
        @DisplayName("子会话（非主会话）时不生成子会话能力提示")
        void childSession_shouldNotGenerateCapabilitiesMessage() {
            ToolConfigDTO childTool = ToolConfigDTO.builder()
                    .name("child_tool").sessionAuth(SessionAuthType.CHILD).build();
            SkillConfigDTO childSkill = SkillConfigDTO.builder()
                    .name("child_skill").sessionAuth(SessionAuthType.CHILD).build();

            var harness = new TestHarness(1L, "sys_prompt",
                    List.of(childTool), List.of(childSkill), null, 99L);
            var captured = executeChat(1L, harness);

            String capsContent = findMessageByContent(getSystemMessages(captured), "子会话可");
            assertNull(capsContent, "子会话自身不应生成子会话能力消息");
        }
    }
}
