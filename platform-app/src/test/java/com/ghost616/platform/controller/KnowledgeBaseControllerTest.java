package com.ghost616.platform.controller;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.ghost616.platform.service.knowledge.KnowledgeBaseService;
import com.ghost616.platform.service.knowledge.KnowledgePublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private KnowledgePublishService knowledgePublishService;

    @InjectMocks
    private KnowledgeBaseController controller;

    private KnowledgeBaseDTO dto(Long id, String name) {
        return KnowledgeBaseDTO.builder().id(id).name(name).status(CommonStatus.ENABLED).build();
    }

    @Test
    void list_应返回ApiResponse成功() {
        when(knowledgeBaseService.list("kb", CommonStatus.ENABLED)).thenReturn(List.of(dto(1L, "kb")));

        ApiResponse<List<KnowledgeBaseDTO>> response = controller.list("kb", CommonStatus.ENABLED);

        assertTrue(response.isSuccess());
        assertEquals("SYS-000", response.getCode());
        assertEquals(1, response.getData().size());
        verify(knowledgeBaseService).list("kb", CommonStatus.ENABLED);
    }

    @Test
    void getById_应返回ApiResponse成功() {
        when(knowledgeBaseService.getById(1L)).thenReturn(dto(1L, "kb"));

        ApiResponse<KnowledgeBaseDTO> response = controller.getById(1L);

        assertTrue(response.isSuccess());
        assertEquals(1L, response.getData().getId());
    }

    @Test
    void create_应传递请求并返回成功() {
        KnowledgeBaseCreateRequest request = KnowledgeBaseCreateRequest.builder().name("kb").build();
        when(knowledgeBaseService.create(request)).thenReturn(dto(1L, "kb"));

        ApiResponse<KnowledgeBaseDTO> response = controller.create(request);

        assertTrue(response.isSuccess());
        assertEquals("kb", response.getData().getName());
    }

    @Test
    void update_应传递id与请求() {
        KnowledgeBaseUpdateRequest request = KnowledgeBaseUpdateRequest.builder().name("new").build();
        when(knowledgeBaseService.update(1L, request)).thenReturn(dto(1L, "new"));

        ApiResponse<KnowledgeBaseDTO> response = controller.update(1L, request);

        assertTrue(response.isSuccess());
        assertEquals("new", response.getData().getName());
        verify(knowledgeBaseService).update(1L, request);
    }

    @Test
    void delete_应返回成功空数据() {
        ApiResponse<Void> response = controller.delete(1L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(knowledgeBaseService).delete(1L);
    }

    @Test
    void toggleStatus_应传递状态() {
        when(knowledgeBaseService.toggleStatus(1L, CommonStatus.DISABLED)).thenReturn(dto(1L, "kb"));

        ApiResponse<KnowledgeBaseDTO> response = controller.toggleStatus(1L, CommonStatus.DISABLED);

        assertTrue(response.isSuccess());
        verify(knowledgeBaseService).toggleStatus(1L, CommonStatus.DISABLED);
    }

    @Test
    void rebuildEs_应调用重建服务() {
        ApiResponse<Void> response = controller.rebuildEs(1L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(knowledgePublishService).rebuildKnowledgeBase(1L);
    }
}
