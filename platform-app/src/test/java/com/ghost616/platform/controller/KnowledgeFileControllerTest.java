package com.ghost616.platform.controller;

import com.ghost616.agentbase.core.ThreadVariableHandler;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;
import com.ghost616.platform.service.knowledge.KnowledgeFileService;
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
class KnowledgeFileControllerTest {

    @Mock
    private KnowledgeFileService knowledgeFileService;

    @Mock
    private KnowledgePublishService knowledgePublishService;

    @Mock
    private ThreadVariableHandler threadVariableHandler;

    @InjectMocks
    private KnowledgeFileController controller;

    private KnowledgeFileDTO dto(Long id, Long kbId) {
        return KnowledgeFileDTO.builder().id(id).knowledgeBaseId(kbId).fileName("a.txt").build();
    }

    @Test
    void list_应传递kbId及过滤参数() {
        when(knowledgeFileService.list(100L, "doc", CommonStatus.ENABLED))
                .thenReturn(List.of(dto(1L, 100L)));

        ApiResponse<List<KnowledgeFileDTO>> response = controller.list(100L, "doc", CommonStatus.ENABLED);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        verify(knowledgeFileService).list(100L, "doc", CommonStatus.ENABLED);
    }

    @Test
    void refresh_应重新查询列表() {
        when(knowledgeFileService.list(100L, "doc", CommonStatus.ENABLED))
                .thenReturn(List.of(dto(1L, 100L)));

        ApiResponse<List<KnowledgeFileDTO>> response = controller.refresh(100L, "doc", CommonStatus.ENABLED);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        verify(knowledgeFileService).list(100L, "doc", CommonStatus.ENABLED);
    }

    @Test
    void publish_应调用发布服务() {
        ApiResponse<Void> response = controller.publish(100L, 1L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(knowledgePublishService).publishFile(eq(1L), any());
    }

    @Test
    void publish_发布中应拒绝重复提交() {
        when(knowledgePublishService.isPublishing(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.publish(100L, 1L));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_PUBLISHING, ex.getErrorCode());
        verify(knowledgePublishService, never()).publishFile(eq(1L), any());
    }

    @Test
    void getById_应返回ApiResponse成功() {
        when(knowledgeFileService.getById(1L)).thenReturn(dto(1L, 100L));

        ApiResponse<KnowledgeFileDTO> response = controller.getById(100L, 1L);

        assertTrue(response.isSuccess());
        assertEquals(1L, response.getData().getId());
    }

    @Test
    void getFileContent_应返回ApiResponse成功() {
        when(knowledgeFileService.getFileContent(1L)).thenReturn("文件内容");

        ApiResponse<String> response = controller.getFileContent(100L, 1L);

        assertTrue(response.isSuccess());
        assertEquals("文件内容", response.getData());
        verify(knowledgeFileService).getFileContent(1L);
    }

    @Test
    void updateFileContent_应传递id与内容() {
        ApiResponse<Void> response = controller.updateFileContent(100L, 1L, "新内容");

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(knowledgeFileService).updateFileContent(1L, "新内容");
    }

    @Test
    void create_应传递路径kbId() {
        KnowledgeFileCreateRequest request = KnowledgeFileCreateRequest.builder().fileName("a.txt").build();
        when(knowledgeFileService.create(100L, request)).thenReturn(dto(1L, 100L));

        ApiResponse<KnowledgeFileDTO> response = controller.create(100L, request);

        assertTrue(response.isSuccess());
        assertEquals(100L, response.getData().getKnowledgeBaseId());
        verify(knowledgeFileService).create(100L, request);
    }

    @Test
    void update_应传递id与请求() {
        KnowledgeFileUpdateRequest request = KnowledgeFileUpdateRequest.builder().fileName("b.txt").build();
        when(knowledgeFileService.update(1L, request)).thenReturn(dto(1L, 100L));

        ApiResponse<KnowledgeFileDTO> response = controller.update(100L, 1L, request);

        assertTrue(response.isSuccess());
        verify(knowledgeFileService).update(1L, request);
    }

    @Test
    void delete_应返回成功空数据() {
        ApiResponse<Void> response = controller.delete(100L, 1L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(knowledgeFileService).delete(1L);
    }

    @Test
    void toggleStatus_应传递状态() {
        when(knowledgeFileService.toggleStatus(1L, CommonStatus.DISABLED)).thenReturn(dto(1L, 100L));

        ApiResponse<KnowledgeFileDTO> response = controller.toggleStatus(100L, 1L, CommonStatus.DISABLED);

        assertTrue(response.isSuccess());
        verify(knowledgeFileService).toggleStatus(1L, CommonStatus.DISABLED);
    }
}
