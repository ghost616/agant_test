package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.RequestType;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.agentinteg.tool.BrowserToolCallback;
import com.ghost616.agentinteg.tool.BrowserToolInvoker;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionSkillMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.service.tool.ToolConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultToolDataProviderTest {

    @Mock private SessionToolMapper sessionToolMapper;
    @Mock private SessionMapper sessionMapper;
    @Mock private AgentSkillMapper agentSkillMapper;
    @Mock private SkillToolMapper skillToolMapper;
    @Mock private SessionSkillMapper sessionSkillMapper;
    @Mock private ToolConfigService toolConfigService;
    @Mock private BrowserToolCallback browserToolCallback;
    @Mock private ModelConfigMapper modelConfigMapper;

    private DefaultToolDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultToolDataProvider(sessionToolMapper, sessionMapper,
                agentSkillMapper, skillToolMapper, sessionSkillMapper, toolConfigService,
                browserToolCallback, modelConfigMapper);
    }

    private SessionTool createSessionTool(Long toolId, SessionAuthType auth) {
        SessionTool st = new SessionTool();
        st.setToolId(toolId);
        st.setSessionAuth(auth);
        return st;
    }

    private ModelConfig createModelConfig(PlatformType platformType, String requestType) {
        ModelConfig mc = new ModelConfig();
        mc.setPlatformType(platformType);
        mc.setRequestType(requestType);
        return mc;
    }

    @Nested
    @DisplayName("getSessionToolIds")
    class GetSessionToolIdsTest {

        @Test
        @DisplayName("返回 List<SessionToolInfo>，包含 toolId 和 sessionAuth")
        void shouldReturnSessionToolInfoList() {
            when(sessionToolMapper.selectList(any())).thenReturn(List.of(
                    createSessionTool(100L, SessionAuthType.CHILD),
                    createSessionTool(101L, SessionAuthType.PARENT)));

            List<SessionToolInfo> result = provider.getSessionToolIds("1");

            assertEquals(2, result.size());
            assertEquals("100", result.get(0).toolId());
            assertEquals(SessionAuthType.CHILD, result.get(0).sessionAuth());
            assertEquals("101", result.get(1).toolId());
            assertEquals(SessionAuthType.PARENT, result.get(1).sessionAuth());
        }

        @Test
        @DisplayName("sessionAuth 为 null 时默认 ALL")
        void nullAuth_shouldDefaultToAll() {
            when(sessionToolMapper.selectList(any())).thenReturn(List.of(
                    createSessionTool(200L, null)));

            List<SessionToolInfo> result = provider.getSessionToolIds("1");

            assertEquals(1, result.size());
            assertEquals("200", result.get(0).toolId());
            assertEquals(SessionAuthType.ALL, result.get(0).sessionAuth());
        }

        @Test
        @DisplayName("无关联工具时返回空列表")
        void emptyTools_shouldReturnEmptyList() {
            when(sessionToolMapper.selectList(any())).thenReturn(List.of());

            List<SessionToolInfo> result = provider.getSessionToolIds("1");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCustomInvoker")
    class GetCustomInvokerTest {

        private final Long toolId = 100L;

        private ToolConfigDTO createToolConfig(ToolType toolType) {
            return ToolConfigDTO.builder()
                    .id(String.valueOf(toolId))
                    .toolType(toolType)
                    .build();
        }

        private ToolDetailDTO createToolDetail(SubToolType subToolType) {
            return ToolDetailDTO.builder()
                    .id(String.valueOf(toolId))
                    .subToolType(subToolType)
                    .build();
        }

        @Test
        @DisplayName("CUSTOM + BROWSER 返回 BrowserToolInvoker")
        void customBrowser_shouldReturnBrowserToolInvoker() {
            ToolConfigDTO config = createToolConfig(ToolType.CUSTOM);
            ToolDetailDTO detail = createToolDetail(SubToolType.BROWSER);
            when(toolConfigService.getById(toolId)).thenReturn(detail);

            CustomToolInvoker result = provider.getCustomInvoker(config);

            assertInstanceOf(BrowserToolInvoker.class, result);
        }

        @Test
        @DisplayName("CUSTOM + BROWSER 返回的 Invoker 包含正确 toolConfig")
        void customBrowser_invokerShouldContainCorrectConfig() {
            ToolConfigDTO config = createToolConfig(ToolType.CUSTOM);
            ToolDetailDTO detail = createToolDetail(SubToolType.BROWSER);
            when(toolConfigService.getById(toolId)).thenReturn(detail);

            CustomToolInvoker result = provider.getCustomInvoker(config);

            assertNotNull(result);
        }

        @Test
        @DisplayName("非 CUSTOM 类型抛出 UnsupportedOperationException")
        void nonCustom_shouldThrow() {
            ToolConfigDTO config = createToolConfig(ToolType.JAVA);
            ToolDetailDTO detail = createToolDetail(SubToolType.BROWSER);
            when(toolConfigService.getById(toolId)).thenReturn(detail);

            assertThrows(UnsupportedOperationException.class,
                    () -> provider.getCustomInvoker(config));
        }

        @Test
        @DisplayName("CUSTOM + 非 BROWSER 子类型抛出 UnsupportedOperationException")
        void customNonBrowser_shouldThrow() {
            ToolConfigDTO config = createToolConfig(ToolType.CUSTOM);
            ToolDetailDTO detail = createToolDetail(null);
            when(toolConfigService.getById(toolId)).thenReturn(detail);

            assertThrows(UnsupportedOperationException.class,
                    () -> provider.getCustomInvoker(config));
        }
    }

    @Nested
    @DisplayName("getBuiltinTools")
    class GetBuiltinToolsTest {

        @Test
        @DisplayName("OPENAI + responses 返回 web_search")
        void openaiResponses_shouldReturnWebSearch() {
            when(modelConfigMapper.selectById(1L))
                    .thenReturn(createModelConfig(PlatformType.OPENAI, RequestType.RESPONSES.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("1");

            assertEquals(1, result.size());
            assertEquals("web_search", result.get(0).get("type"));
        }

        @Test
        @DisplayName("DEEPSEEK + responses_stateless 返回 web_search")
        void deepseekResponsesStateless_shouldReturnWebSearch() {
            when(modelConfigMapper.selectById(2L))
                    .thenReturn(createModelConfig(PlatformType.DEEPSEEK, RequestType.RESPONSES_STATELESS.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("2");

            assertEquals(1, result.size());
            assertEquals("web_search", result.get(0).get("type"));
        }

        @Test
        @DisplayName("OPENAI + completions 返回空列表")
        void openaiCompletions_shouldReturnEmpty() {
            when(modelConfigMapper.selectById(1L))
                    .thenReturn(createModelConfig(PlatformType.OPENAI, RequestType.COMPLETIONS.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("1");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("ANTHROPIC + responses 返回空列表")
        void anthropicResponses_shouldReturnEmpty() {
            when(modelConfigMapper.selectById(3L))
                    .thenReturn(createModelConfig(PlatformType.ANTHROPIC, RequestType.RESPONSES.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("3");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("模型不存在时返回空列表")
        void modelNotFound_shouldReturnEmpty() {
            when(modelConfigMapper.selectById(99L)).thenReturn(null);

            List<Map<String, Object>> result = provider.getBuiltinTools("99");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("modelId 为 null 时返回空列表且不查询数据库")
        void nullModelId_shouldReturnEmpty() {
            List<Map<String, Object>> result = provider.getBuiltinTools(null);

            assertTrue(result.isEmpty());
            verify(modelConfigMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("KIMI + requestType 为 null 返回 builtin_function")
        void kimiNullRequestType_shouldReturnBuiltinFunction() {
            when(modelConfigMapper.selectById(4L))
                    .thenReturn(createModelConfig(PlatformType.KIMI, null));

            List<Map<String, Object>> result = provider.getBuiltinTools("4");

            assertEquals(1, result.size());
            assertEquals("builtin_function", result.get(0).get("type"));
            assertEquals("$web_search",
                    ((Map<?, ?>) result.get(0).get("function")).get("name"));
        }

        @Test
        @DisplayName("KIMI + requestType 为空字符串返回 builtin_function")
        void kimiEmptyRequestType_shouldReturnBuiltinFunction() {
            when(modelConfigMapper.selectById(4L))
                    .thenReturn(createModelConfig(PlatformType.KIMI, ""));

            List<Map<String, Object>> result = provider.getBuiltinTools("4");

            assertEquals(1, result.size());
            assertEquals("builtin_function", result.get(0).get("type"));
            assertEquals("$web_search",
                    ((Map<?, ?>) result.get(0).get("function")).get("name"));
        }

        @Test
        @DisplayName("KIMI + requestType 为 completions 返回 builtin_function")
        void kimiCompletions_shouldReturnBuiltinFunction() {
            when(modelConfigMapper.selectById(4L))
                    .thenReturn(createModelConfig(PlatformType.KIMI, RequestType.COMPLETIONS.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("4");

            assertEquals(1, result.size());
            assertEquals("builtin_function", result.get(0).get("type"));
            assertEquals("$web_search",
                    ((Map<?, ?>) result.get(0).get("function")).get("name"));
        }

        @Test
        @DisplayName("KIMI + responses 返回空列表")
        void kimiResponses_shouldReturnEmpty() {
            when(modelConfigMapper.selectById(4L))
                    .thenReturn(createModelConfig(PlatformType.KIMI, RequestType.RESPONSES.getCode()));

            List<Map<String, Object>> result = provider.getBuiltinTools("4");

            assertTrue(result.isEmpty());
        }
    }
}
