package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.agent_log.AgentLogDTO;
import com.ghost616.platform.service.agent_log.AgentLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentLogControllerTest {

    @Mock
    private AgentLogService agentLogService;

    @Test
    void 类注解_为RestController且映射agentLogs路径() throws NoSuchMethodException {
        AgentLogController controller = new AgentLogController(agentLogService);
        assertNotNull(controller.getClass().getAnnotation(RestController.class));
        RequestMapping requestMapping = controller.getClass().getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/api/agent-logs"}, requestMapping.value());
        assertNotNull(controller.getClass().getDeclaredMethod("list", Long.class, Long.class, String.class, String.class,
                String.class, String.class, int.class, int.class).getAnnotation(GetMapping.class));
    }

    @Test
    void list_请求参数正确透传service并返回success() {
        PageResult<AgentLogDTO> pageResult = new PageResult<>(List.of(), 0, 1, 20);
        when(agentLogService.list(10L, null, "测试会话", "conv-1", "MODEL_CALL", "INFO", 1, 20)).thenReturn(pageResult);

        AgentLogController controller = new AgentLogController(agentLogService);
        ApiResponse<PageResult<AgentLogDTO>> response = controller.list(10L, null, "测试会话", "conv-1", "MODEL_CALL", "INFO", 1, 20);

        assertTrue(response.isSuccess());
        assertEquals("SYS-000", response.getCode());
        assertSame(pageResult, response.getData());
        verify(agentLogService).list(10L, null, "测试会话", "conv-1", "MODEL_CALL", "INFO", 1, 20);
    }

    @Test
    void list_默认参数page1size20() {
        PageResult<AgentLogDTO> pageResult = new PageResult<>(List.of(), 0, 1, 20);
        when(agentLogService.list(null, null, null, null, null, null, 1, 20)).thenReturn(pageResult);

        AgentLogController controller = new AgentLogController(agentLogService);
        ApiResponse<PageResult<AgentLogDTO>> response = controller.list(null, null, null, null, null, null, 1, 20);

        assertTrue(response.isSuccess());
        verify(agentLogService).list(null, null, null, null, null, null, 1, 20);
    }

    @Test
    void list_sessionName参数正确透传service() {
        PageResult<AgentLogDTO> pageResult = new PageResult<>(List.of(), 0, 1, 20);
        when(agentLogService.list(null, null, "财务", null, null, null, 1, 20)).thenReturn(pageResult);

        AgentLogController controller = new AgentLogController(agentLogService);
        ApiResponse<PageResult<AgentLogDTO>> response = controller.list(null, null, "财务", null, null, null, 1, 20);

        assertTrue(response.isSuccess());
        verify(agentLogService).list(null, null, "财务", null, null, null, 1, 20);
    }

    @Test
    void list_rootSessionId参数正确透传service() {
        PageResult<AgentLogDTO> pageResult = new PageResult<>(List.of(), 0, 1, 20);
        when(agentLogService.list(null, 50L, null, null, null, null, 1, 20)).thenReturn(pageResult);

        AgentLogController controller = new AgentLogController(agentLogService);
        ApiResponse<PageResult<AgentLogDTO>> response = controller.list(null, 50L, null, null, null, null, 1, 20);

        assertTrue(response.isSuccess());
        verify(agentLogService).list(null, 50L, null, null, null, null, 1, 20);
    }
}
