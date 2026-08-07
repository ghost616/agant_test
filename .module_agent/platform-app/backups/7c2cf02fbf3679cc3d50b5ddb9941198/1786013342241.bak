package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.enums.PublishStatus;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeFileServiceImplTest {

    @Mock
    private KnowledgeFileMapper knowledgeFileMapper;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private KnowledgeFileServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeBase.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeFile.class);
        service = new KnowledgeFileServiceImpl(knowledgeFileMapper, knowledgeBaseMapper);
    }

    private KnowledgeFile fileEntity(Long id, Long kbId, String name) {
        KnowledgeFile f = new KnowledgeFile();
        f.setId(id);
        f.setKnowledgeBaseId(kbId);
        f.setFileName(name);
        f.setStatus(CommonStatus.ENABLED);
        f.setFileSize(10L);
        f.setLineCount(2);
        return f;
    }

    @Test
    @DisplayName("create() 校验知识库存在，不计算 fileSize/lineCount")
    void create_校验知识库存在() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb);

        KnowledgeFileCreateRequest request = KnowledgeFileCreateRequest.builder()
                .fileName("doc.txt")
                .fileDescription("描述")
                .build();
        KnowledgeFileDTO result = service.create(100L, request);

        ArgumentCaptor<KnowledgeFile> captor = ArgumentCaptor.forClass(KnowledgeFile.class);
        verify(knowledgeFileMapper).insert((KnowledgeFile) captor.capture());
        KnowledgeFile saved = captor.getValue();
        assertEquals("doc.txt", saved.getFileName());
        assertEquals("描述", saved.getFileDescription());
        assertEquals(100L, saved.getKnowledgeBaseId());
        assertEquals(CommonStatus.ENABLED, saved.getStatus(), "create 默认状态应为 ENABLED");
        assertEquals(PublishStatus.UNPUBLISHED, saved.getPublishStatus(), "create 默认发布状态应为 UNPUBLISHED");
        assertNull(saved.getFileContent(), "create 不应设置 fileContent");
        assertNull(saved.getFileSize(), "create 不应计算 fileSize");
        assertNull(saved.getLineCount(), "create 不应计算 lineCount");
        assertEquals(CommonStatus.ENABLED, result.getStatus());
        assertEquals(PublishStatus.UNPUBLISHED, result.getPublishStatus());
        assertEquals(100L, result.getKnowledgeBaseId());
    }

    @Test
    @DisplayName("updateFileContent() 按 UTF-8 字节计算 fileSize，按换行符计算 lineCount")
    void updateFileContent_计算指标() {
        KnowledgeFile existing = fileEntity(1L, 100L, "a.txt");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        String content = "你好\n世界";
        service.updateFileContent(1L, content);

        verify(knowledgeFileMapper).updateById((KnowledgeFile) existing);
        assertEquals(content, existing.getFileContent());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, existing.getFileSize());
        assertEquals(2, existing.getLineCount());
    }

    @Test
    @DisplayName("updateFileContent() 文件不存在抛 KNOWLEDGE_FILE_NOT_FOUND")
    void updateFileContent_不存在() {
        when(knowledgeFileMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateFileContent(1L, "内容"));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());
        verify(knowledgeFileMapper, never()).updateById(any(KnowledgeFile.class));
    }

    @Test
    @DisplayName("updateFileContent() 已发布文件修改内容后 publishStatus 变为 PENDING_PUBLISH")
    void updateFileContent_已发布转待发布() {
        KnowledgeFile existing = fileEntity(1L, 100L, "a.txt");
        existing.setPublishStatus(PublishStatus.PUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        service.updateFileContent(1L, "新内容");

        assertEquals(PublishStatus.PENDING_PUBLISH, existing.getPublishStatus(), "已发布文件改内容后应置为 PENDING_PUBLISH");
    }

    @Test
    @DisplayName("updateFileContent() 非已发布文件不改变 publishStatus")
    void updateFileContent_未发布不改状态() {
        KnowledgeFile existing = fileEntity(1L, 100L, "a.txt");
        existing.setPublishStatus(PublishStatus.UNPUBLISHED);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        service.updateFileContent(1L, "新内容");

        assertEquals(PublishStatus.UNPUBLISHED, existing.getPublishStatus());
    }

    @Test
    @DisplayName("getFileContent() 返回文件内容，不存在抛异常")
    void getFileContent_行为() {
        KnowledgeFile f = fileEntity(1L, 100L, "a.txt");
        f.setFileContent("文件内容");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);
        assertEquals("文件内容", service.getFileContent(1L));

        when(knowledgeFileMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getFileContent(2L));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("create() 知识库不存在抛 KNOWLEDGE_BASE_NOT_FOUND")
    void create_知识库不存在() {
        when(knowledgeBaseMapper.selectById(9L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(9L, KnowledgeFileCreateRequest.builder().fileName("f").build()));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
        verify(knowledgeFileMapper, never()).insert(any(KnowledgeFile.class));
    }

    @Test
    @DisplayName("update() 更新基础字段，不重算 fileSize/lineCount")
    void update_更新基础字段() {
        KnowledgeFile existing = fileEntity(1L, 100L, "old.txt");
        existing.setFileSize(99L);
        existing.setLineCount(9);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        KnowledgeFileUpdateRequest request = KnowledgeFileUpdateRequest.builder()
                .fileName("new.txt")
                .build();
        KnowledgeFileDTO result = service.update(1L, request);

        verify(knowledgeFileMapper).updateById((KnowledgeFile) existing);
        assertEquals("new.txt", existing.getFileName());
        assertEquals(99L, existing.getFileSize(), "update 不应重算 fileSize");
        assertEquals(9, existing.getLineCount(), "update 不应重算 lineCount");
        assertEquals("new.txt", result.getFileName());
    }

    @Test
    @DisplayName("update() 更新 fileDescription")
    void update_更新描述() {
        KnowledgeFile existing = fileEntity(1L, 100L, "old.txt");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        service.update(1L, KnowledgeFileUpdateRequest.builder().fileDescription("新描述").build());

        verify(knowledgeFileMapper).updateById((KnowledgeFile) existing);
        assertEquals("新描述", existing.getFileDescription());
    }

    @Test
    @DisplayName("update() 文件不存在抛 KNOWLEDGE_FILE_NOT_FOUND")
    void update_不存在() {
        when(knowledgeFileMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(1L, KnowledgeFileUpdateRequest.builder().build()));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getById() 正常返回 DTO，不含 fileContent")
    void getById_正常返回() {
        KnowledgeFile f = fileEntity(1L, 100L, "a.txt");
        f.setFileContent("内容");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);

        KnowledgeFileDTO result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals(100L, result.getKnowledgeBaseId());
        assertEquals("a.txt", result.getFileName());
    }

    @Test
    @DisplayName("getById() 不存在抛 KNOWLEDGE_FILE_NOT_FOUND")
    void getById_不存在() {
        when(knowledgeFileMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(1L));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("delete() 不存在抛 KNOWLEDGE_FILE_NOT_FOUND，存在则删除")
    void delete_行为() {
        when(knowledgeFileMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());

        when(knowledgeFileMapper.selectById(2L)).thenReturn(fileEntity(2L, 100L, "b.txt"));
        service.delete(2L);
        verify(knowledgeFileMapper).deleteById(2L);
    }

    @Test
    @DisplayName("toggleStatus() 更新状态，不存在抛异常")
    void toggleStatus_行为() {
        when(knowledgeFileMapper.selectById(1L)).thenReturn(fileEntity(1L, 100L, "a.txt"));
        KnowledgeFileDTO result = service.toggleStatus(1L, CommonStatus.DISABLED);
        verify(knowledgeFileMapper).updateById(any(KnowledgeFile.class));
        assertEquals(CommonStatus.DISABLED, result.getStatus());

        when(knowledgeFileMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleStatus(2L, CommonStatus.DISABLED));
        assertEquals(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("list() 按 knowledgeBaseId 过滤")
    void list_按知识库过滤() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of(fileEntity(1L, 100L, "a.txt")));

        service.list(100L, null, null);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeFile>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql != null && sql.contains("knowledge_base_id"));
    }

    @Test
    @DisplayName("list() fileName/status 组合过滤")
    void list_组合过滤() {
        when(knowledgeFileMapper.selectList(any())).thenReturn(List.of());

        service.list(100L, "doc", CommonStatus.ENABLED);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeFile>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql != null && sql.contains("file_name"));
        assertTrue(sql != null && sql.contains("status"));
    }
}
