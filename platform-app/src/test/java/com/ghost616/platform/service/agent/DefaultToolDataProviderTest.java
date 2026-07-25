package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import com.ghost616.agentinteg.tool.BrowserToolCallback;
import com.ghost616.agentinteg.tool.BrowserToolInvoker;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.repository.AgentSkillMapper;
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

    private DefaultToolDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultToolDataProvider(sessionToolMapper, sessionMapper,
                agentSkillMapper, skillToolMapper, sessionSkillMapper, toolConfigService,
                browserToolCallback);
    }

    private SessionTool createSessionTool(Long toolId, SessionAuthType auth) {
        SessionTool st = new SessionTool();
        st.setToolId(toolId);
        st.setSessionAuth(auth);
        return st;
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

            List<SessionToolInfo> result = provider.getSessionToolIds(1L);

            assertEquals(2, result.size());
            assertEquals(100L, result.get(0).toolId());
            assertEquals(SessionAuthType.CHILD, result.get(0).sessionAuth());
            assertEquals(101L, result.get(1).toolId());
            assertEquals(SessionAuthType.PARENT, result.get(1).sessionAuth());
        }

        @Test
        @DisplayName("sessionAuth 为 null 时默认 ALL")
        void nullAuth_shouldDefaultToAll() {
            when(sessionToolMapper.selectList(any())).thenReturn(List.of(
                    createSessionTool(200L, null)));

            List<SessionToolInfo> result = provider.getSessionToolIds(1L);

            assertEquals(1, result.size());
            assertEquals(200L, result.get(0).toolId());
            assertEquals(SessionAuthType.ALL, result.get(0).sessionAuth());
        }

        @Test
        @DisplayName("无关联工具时返回空列表")
        void emptyTools_shouldReturnEmptyList() {
            when(sessionToolMapper.selectList(any())).thenReturn(List.of());

            List<SessionToolInfo> result = provider.getSessionToolIds(1L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCustomInvoker")
    class GetCustomInvokerTest {

        private final Long toolId = 100L;

        private ToolConfigDTO createToolConfig(ToolType toolType) {
            return ToolConfigDTO.builder()
                    .id(toolId)
                    .toolType(toolType)
                    .build();
        }

        private ToolDetailDTO createToolDetail(SubToolType subToolType) {
            return ToolDetailDTO.builder()
                    .id(toolId)
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
}
