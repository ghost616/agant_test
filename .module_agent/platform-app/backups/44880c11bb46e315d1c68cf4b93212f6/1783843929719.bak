package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.ApiResponse;
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

    @InjectMocks
    private ToolExecutionController controller;

    private final Long sessionId = 1L;

    @Test
    void executeTools_委托服务并返回ApiResponse() {
        ToolExecutionService.ToolExecutionResult serviceResult =
                new ToolExecutionService.ToolExecutionResult("executing", "tc-1", "testTool", "{}", false, null);
        when(toolExecutionService.executeTool(sessionId)).thenReturn(serviceResult);

        ApiResponse<ToolExecutionService.ToolExecutionResult> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("executing", response.getData().status());
        assertEquals("tc-1", response.getData().toolId());
        assertEquals("testTool", response.getData().toolName());
        verify(toolExecutionService).executeTool(sessionId);
    }

    @Test
    void executeTools_返回empty状态() {
        ToolExecutionService.ToolExecutionResult serviceResult =
                new ToolExecutionService.ToolExecutionResult("empty", null, null, null, false, null);
        when(toolExecutionService.executeTool(sessionId)).thenReturn(serviceResult);

        ApiResponse<ToolExecutionService.ToolExecutionResult> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("empty", response.getData().status());
        assertFalse(response.getData().hasMore());
        verify(toolExecutionService).executeTool(sessionId);
    }

    @Test
    void toolStatus_返回状态结果() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("done", "tc-1", "testTool", "{}", false, "ok");
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);

        ApiResponse<ToolExecutionService.ToolStatusResult> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertEquals("done", response.getData().status());
        assertEquals("ok", response.getData().result());
        verify(toolExecutionService).getToolStatus(sessionId, "tc-1");
    }

    @Test
    void toolStatus_返回idle状态() {
        ToolExecutionService.ToolStatusResult serviceResult =
                new ToolExecutionService.ToolStatusResult("idle", null, null, null, false, null);
        when(toolExecutionService.getToolStatus(sessionId, "tc-1")).thenReturn(serviceResult);

        ApiResponse<ToolExecutionService.ToolStatusResult> response = controller.toolStatus(sessionId, "tc-1");

        assertTrue(response.isSuccess());
        assertEquals("idle", response.getData().status());
        verify(toolExecutionService).getToolStatus(sessionId, "tc-1");
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
