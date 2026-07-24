package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.ToolStatusResultDTO;
import com.ghost616.platform.entity.ToolConfig;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;
import com.ghost616.platform.service.tool.ToolConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ToolExecutionControllerTest {

    @Mock
    private ToolExecutionService toolExecutionService;

    @Mock
    private DefaultSubSessionCallback defaultSubSessionCallback;

    @Mock
    private ToolConfigService toolConfigService;

    @InjectMocks
    private ToolExecutionController controller;

    private final Long sessionId = 1L;

    @Test
    void executeTools_委托服务并返回ToolStatusResultDTO() {
        ToolExecutionService.ToolExecutionResult serviceResult =
                new ToolExecutionService.ToolExecutionResult("executing", "tc-1", "testTool", "{}", false, null);
        when(toolExecutionService.executeTool(sessionId)).thenReturn(serviceResult);

        ApiResponse<ToolStatusResultDTO> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("executing", response.getData().getStatus());
        assertEquals("tc-1", response.getData().getToolId());
        assertEquals("testTool", response.getData().getToolName());
        assertFalse(response.getData().isNeedsSubSessionFlow());
        verify(toolExecutionService).executeTool(sessionId);
        verifyNoInteractions(defaultSubSessionCallback);
    }

    @Test
    void executeTools_返回empty状态() {
        ToolExecutionService.ToolExecutionResult serviceResult =
                new ToolExecutionService.ToolExecutionResult("empty", null, null, null, false, null);
        when(toolExecutionService.executeTool(sessionId)).thenReturn(serviceResult);

        ApiResponse<ToolStatusResultDTO> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("empty", response.getData().getStatus());
        assertFalse(response.getData().isHasMore());
        verify(toolExecutionService).executeTool(sessionId);
    }

    @Test
    void toolStatus_返回状态结果() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "testTool", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertEquals("done", response.getData().getStatus());
        assertEquals("ok", response.getData().getResult());
        assertFalse(response.getData().isNeedsSubSessionFlow());
        verify(toolExecutionService).getToolStatus(sessionId, "tc-1");
        verifyNoInteractions(defaultSubSessionCallback);
    }

    @Test
    void toolStatus_返回idle状态() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("idle", null, null, null, false, null);
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertEquals("idle", response.getData().getStatus());
        verify(toolExecutionService).getToolStatus(sessionId, "tc-1");
    }

    @Test
    void toolStatus_检测子会话回调设置needsSubSessionFlow() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "_sys_callback_sub_session", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        DefaultSubSessionCallback.SubSessionData subData = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(defaultSubSessionCallback.getSubSessionData(sessionId)).thenReturn(subData);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isNeedsSubSessionFlow());
        verify(defaultSubSessionCallback).getSubSessionData(sessionId);
    }

    @Test
    void toolStatus_子会话回调无数据时不设置needsSubSessionFlow() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "_sys_callback_sub_session", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        when(defaultSubSessionCallback.getSubSessionData(sessionId)).thenReturn(null);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isNeedsSubSessionFlow());
        verify(defaultSubSessionCallback).getSubSessionData(sessionId);
    }

    @Test
    void toolStatus_ToolConfig正常时正确构建ToolConfigBrief() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "browser-tool", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        ToolConfig toolConfig = new ToolConfig();
        toolConfig.setId(100L);
        toolConfig.setName("browser-tool");
        toolConfig.setSubToolType(SubToolType.BROWSER);
        when(toolConfigService.getToolConfigBySessionAndName(sessionId, "browser-tool")).thenReturn(toolConfig);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        ToolStatusResultDTO.ToolConfigBrief brief = response.getData().getToolConfig();
        assertNotNull(brief);
        assertEquals("100", brief.getId());
        assertEquals("BROWSER", brief.getSubToolType());
        assertEquals("browser-tool", brief.getToolName());
        verify(toolConfigService).getToolConfigBySessionAndName(sessionId, "browser-tool");
    }

    @Test
    void toolStatus_ToolConfig为null时toolConfig保持null() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "unknown-tool", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        when(toolConfigService.getToolConfigBySessionAndName(sessionId, "unknown-tool")).thenReturn(null);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertNull(response.getData().getToolConfig());
        verify(toolConfigService).getToolConfigBySessionAndName(sessionId, "unknown-tool");
    }

    @Test
    void toolStatus_SubToolType为null时brief中subToolType保持null() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "no-subtype-tool", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        ToolConfig toolConfig = new ToolConfig();
        toolConfig.setId(200L);
        toolConfig.setName("no-subtype-tool");
        toolConfig.setSubToolType(null);
        when(toolConfigService.getToolConfigBySessionAndName(sessionId, "no-subtype-tool")).thenReturn(toolConfig);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        ToolStatusResultDTO.ToolConfigBrief brief = response.getData().getToolConfig();
        assertNotNull(brief);
        assertEquals("200", brief.getId());
        assertNull(brief.getSubToolType());
        assertEquals("no-subtype-tool", brief.getToolName());
        verify(toolConfigService).getToolConfigBySessionAndName(sessionId, "no-subtype-tool");
    }

    @Test
    void toolStatus_原有子会话回调逻辑不受影响() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "_sys_callback_sub_session", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);
        DefaultSubSessionCallback.SubSessionData subData = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(defaultSubSessionCallback.getSubSessionData(sessionId)).thenReturn(subData);
        when(toolConfigService.getToolConfigBySessionAndName(sessionId, "_sys_callback_sub_session")).thenReturn(null);

        ApiResponse<ToolStatusResultDTO> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.getData().isNeedsSubSessionFlow());
        assertNull(response.getData().getToolConfig());
        verify(defaultSubSessionCallback).getSubSessionData(sessionId);
        verify(toolConfigService).getToolConfigBySessionAndName(sessionId, "_sys_callback_sub_session");
    }

    @Test
    void continueChat_委托服务并返回Flux() {
        ChatChunk chunk = ChatChunk.builder().delta("continue").build();
        Flux<ServerSentEvent<ChatChunk>> expectedFlux =
                Flux.just(ServerSentEvent.builder(chunk).build());
        when(toolExecutionService.continueAfterTools(sessionId)).thenReturn(expectedFlux);

        Flux<ServerSentEvent<ChatChunk>> result = controller.continueChat(sessionId);

        StepVerifier.create(result)
                .expectNextMatches(sse -> "continue".equals(sse.data().getDelta()))
                .expectComplete()
                .verify();
        verify(toolExecutionService).continueAfterTools(sessionId);
    }

    @Test
    void continueChat_stopped返回空Flux() {
        when(toolExecutionService.continueAfterTools(sessionId)).thenReturn(Flux.empty());

        Flux<ServerSentEvent<ChatChunk>> result = controller.continueChat(sessionId);

        StepVerifier.create(result)
                .expectComplete()
                .verify();
        verify(toolExecutionService).continueAfterTools(sessionId);
    }
}
