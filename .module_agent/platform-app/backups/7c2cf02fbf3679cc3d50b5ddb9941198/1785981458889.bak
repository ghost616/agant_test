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
    @DisplayName("create() 校验知识库存在并计算 fileSize/lineCount")
    void create_校验并计算指标() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);
        when(knowledgeBaseMapper.selectById(100L)).thenReturn(kb);

        String content = "第一行\n第二行";
        KnowledgeFileCreateRequest request = KnowledgeFileCreateRequest.builder()
                .fileName("doc.txt")
                .fileDescription("描述")
                .fileContent(content)
                .build();
        KnowledgeFileDTO result = service.create(100L, request);

        ArgumentCaptor<KnowledgeFile> captor = ArgumentCaptor.forClass(KnowledgeFile.class);
        verify(knowledgeFileMapper).insert((KnowledgeFile) captor.capture());
        KnowledgeFile saved = captor.getValue();
        assertEquals("doc.txt", saved.getFileName());
        assertEquals("描述", saved.getFileDescription());
        assertEquals(100L, saved.getKnowledgeBaseId());
        assertEquals(CommonStatus.ENABLED, saved.getStatus(), "create 默认状态应为 ENABLED");
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, saved.getFileSize(), "fileSize 应为 UTF-8 字节数");
        assertEquals(2, saved.getLineCount(), "lineCount 应按换行符计数");
        assertEquals(CommonStatus.ENABLED, result.getStatus());
        assertEquals(100L, result.getKnowledgeBaseId());
    }

    @Test
    @DisplayName("create() 中文内容按 UTF-8 字节计算 fileSize")
    void create_UTF8字节数() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);

        String content = "你好世界";
        service.create(1L, KnowledgeFileCreateRequest.builder().fileName("c.txt").fileContent(content).build());

        ArgumentCaptor<KnowledgeFile> captor = ArgumentCaptor.forClass(KnowledgeFile.class);
        verify(knowledgeFileMapper).insert((KnowledgeFile) captor.capture());
        assertEquals("你好世界".getBytes(StandardCharsets.UTF_8).length, captor.getValue().getFileSize());
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
    @DisplayName("update() fileContent 非空时重算 fileSize/lineCount")
    void update_重算指标() {
        KnowledgeFile existing = fileEntity(1L, 100L, "old.txt");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        String newContent = "a\nb\nc";
        KnowledgeFileUpdateRequest request = KnowledgeFileUpdateRequest.builder()
                .fileName("new.txt")
                .fileContent(newContent)
                .build();
        KnowledgeFileDTO result = service.update(1L, request);

        verify(knowledgeFileMapper).updateById((KnowledgeFile) existing);
        assertEquals("new.txt", existing.getFileName());
        assertEquals(newContent, existing.getFileContent());
        assertEquals(newContent.getBytes(StandardCharsets.UTF_8).length, existing.getFileSize());
        assertEquals(3, existing.getLineCount());
        assertEquals("new.txt", result.getFileName());
    }

    @Test
    @DisplayName("update() fileContent 为 null 时不重算指标")
    void update_fileContent为空() {
        KnowledgeFile existing = fileEntity(1L, 100L, "old.txt");
        existing.setFileSize(99L);
        existing.setLineCount(9);
        when(knowledgeFileMapper.selectById(1L)).thenReturn(existing);

        service.update(1L, KnowledgeFileUpdateRequest.builder().status(CommonStatus.DISABLED).build());

        verify(knowledgeFileMapper).updateById((KnowledgeFile) existing);
        assertEquals(CommonStatus.DISABLED, existing.getStatus());
        assertEquals(99L, existing.getFileSize(), "fileContent 为 null 不应重算 fileSize");
        assertEquals(9, existing.getLineCount(), "fileContent 为 null 不应重算 lineCount");
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
    @DisplayName("getById() 正常返回含 fileContent")
    void getById_正常返回() {
        KnowledgeFile f = fileEntity(1L, 100L, "a.txt");
        f.setFileContent("内容");
        when(knowledgeFileMapper.selectById(1L)).thenReturn(f);

        KnowledgeFileDTO result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals(100L, result.getKnowledgeBaseId());
        assertEquals("a.txt", result.getFileName());
        assertEquals("内容", result.getFileContent());
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
