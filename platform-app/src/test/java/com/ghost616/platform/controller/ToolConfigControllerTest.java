package com.ghost616.platform.controller;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.service.tool.ToolConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolConfigControllerTest {

    @Mock
    private ToolConfigService toolConfigService;

    @InjectMocks
    private ToolConfigController controller;

    @Test
    void list_shouldReturnToolDetailDTOList() {
        ToolDetailDTO dto = ToolDetailDTO.builder().id(1L).name("test").build();
        when(toolConfigService.list(null, null, null)).thenReturn(List.of(dto));

        ApiResponse<List<ToolDetailDTO>> response = controller.list(null, null, null);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("test", response.getData().get(0).getName());
    }

    @Test
    void getById_shouldReturnToolDetailDTO() {
        ToolDetailDTO dto = ToolDetailDTO.builder().id(1L).name("test").build();
        when(toolConfigService.getById(1L)).thenReturn(dto);

        ApiResponse<ToolDetailDTO> response = controller.getById(1L);

        assertTrue(response.isSuccess());
        assertEquals("test", response.getData().getName());
    }

    @Test
    void create_shouldReturnToolDetailDTO() {
        ToolDetailDTO dto = ToolDetailDTO.builder()
                .id(1L).name("browser_tool")
                .subToolType(SubToolType.BROWSER)
                .toolScript("script")
                .build();
        ToolCreateRequest request = ToolCreateRequest.builder().name("browser_tool").build();
        when(toolConfigService.create(request)).thenReturn(dto);

        ApiResponse<ToolDetailDTO> response = controller.create(request);

        assertTrue(response.isSuccess());
        assertEquals("browser_tool", response.getData().getName());
        assertEquals(SubToolType.BROWSER, response.getData().getSubToolType());
        assertEquals("script", response.getData().getToolScript());
    }

    @Test
    void update_shouldReturnToolDetailDTO() {
        ToolDetailDTO dto = ToolDetailDTO.builder()
                .id(1L).name("updated_tool")
                .subToolType(SubToolType.BROWSER)
                .build();
        ToolUpdateRequest request = ToolUpdateRequest.builder().name("updated_tool").build();
        when(toolConfigService.update(1L, request)).thenReturn(dto);

        ApiResponse<ToolDetailDTO> response = controller.update(1L, request);

        assertTrue(response.isSuccess());
        assertEquals("updated_tool", response.getData().getName());
    }

    @Test
    void delete_shouldSucceed() {
        doNothing().when(toolConfigService).delete(1L);

        ApiResponse<Void> response = controller.delete(1L);

        assertTrue(response.isSuccess());
        verify(toolConfigService).delete(1L);
    }

    @Test
    void getImplByName_shouldReturnToolDetailDTO() {
        ToolDetailDTO dto = ToolDetailDTO.builder().id(1L).name("test_tool").build();
        when(toolConfigService.getImplByName("test_tool")).thenReturn(dto);

        ApiResponse<ToolDetailDTO> response = controller.getImplByName("test_tool");

        assertTrue(response.isSuccess());
        assertEquals("test_tool", response.getData().getName());
    }

    @Test
    void toggleStatus_shouldReturnToolDetailDTO() {
        ToolDetailDTO dto = ToolDetailDTO.builder().id(1L).name("test").status(CommonStatus.DISABLED).build();
        when(toolConfigService.toggleStatus(1L, CommonStatus.DISABLED)).thenReturn(dto);

        ApiResponse<ToolDetailDTO> response = controller.toggleStatus(1L, CommonStatus.DISABLED);

        assertTrue(response.isSuccess());
        assertEquals(CommonStatus.DISABLED, response.getData().getStatus());
    }
}
