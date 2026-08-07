package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeFileMapper knowledgeFileMapper;

    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeBase.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeFile.class);
        service = new KnowledgeBaseServiceImpl(knowledgeBaseMapper, knowledgeFileMapper);
    }

    private KnowledgeBase entity(Long id, String name, CommonStatus status) {
        KnowledgeBase e = new KnowledgeBase();
        e.setId(id);
        e.setName(name);
        e.setDescription("desc-" + name);
        e.setStatus(status);
        return e;
    }

    @Test
    @DisplayName("create() 默认 status=ENABLED 且插入实体")
    void create_默认ENABLED() {
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);

        KnowledgeBaseCreateRequest request = KnowledgeBaseCreateRequest.builder()
                .name("kb-1")
                .description("知识库描述")
                .build();
        KnowledgeBaseDTO result = service.create(request);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).insert((KnowledgeBase) captor.capture());
        KnowledgeBase saved = captor.getValue();
        assertEquals("kb-1", saved.getName());
        assertEquals("知识库描述", saved.getDescription());
        assertEquals(CommonStatus.ENABLED, saved.getStatus(), "create 默认状态应为 ENABLED");
        assertEquals(CommonStatus.ENABLED, result.getStatus());
        assertEquals("kb-1", result.getName());
    }

    @Test
    @DisplayName("create() 名称重复抛 KNOWLEDGE_BASE_ALREADY_EXISTS")
    void create_名称重复抛异常() {
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(1L);

        KnowledgeBaseCreateRequest request = KnowledgeBaseCreateRequest.builder().name("dup").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_ALREADY_EXISTS, ex.getErrorCode());
        verify(knowledgeBaseMapper, never()).insert(any(KnowledgeBase.class));
    }

    @Test
    @DisplayName("update() name 非空时校验唯一性并更新")
    void update_name非空更新() {
        KnowledgeBase existing = entity(1L, "old", CommonStatus.ENABLED);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(existing);
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);

        KnowledgeBaseUpdateRequest request = KnowledgeBaseUpdateRequest.builder()
                .name("new-name")
                .description("新描述")
                .build();
        KnowledgeBaseDTO result = service.update(1L, request);

        verify(knowledgeBaseMapper).updateById((KnowledgeBase) existing);
        assertEquals("new-name", existing.getName());
        assertEquals("新描述", existing.getDescription());
        assertEquals("new-name", result.getName());
    }

    @Test
    @DisplayName("update() 名称重复时抛 KNOWLEDGE_BASE_ALREADY_EXISTS")
    void update_名称重复抛异常() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(entity(1L, "old", CommonStatus.ENABLED));
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(1L);

        KnowledgeBaseUpdateRequest request = KnowledgeBaseUpdateRequest.builder().name("dup").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_ALREADY_EXISTS, ex.getErrorCode());
        verify(knowledgeBaseMapper, never()).updateById(any(KnowledgeBase.class));
    }

    @Test
    @DisplayName("update() 知识库不存在抛 KNOWLEDGE_BASE_NOT_FOUND")
    void update_不存在抛异常() {
        when(knowledgeBaseMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(99L, KnowledgeBaseUpdateRequest.builder().build()));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("delete() 级联删除该知识库下的所有文件")
    void delete_级联删除文件() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(entity(1L, "kb", CommonStatus.ENABLED));

        service.delete(1L);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeFile>> fileWrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeFileMapper).delete(fileWrapperCaptor.capture());
        assertNotNull(fileWrapperCaptor.getValue().getSqlSegment());
        assertTrue(fileWrapperCaptor.getValue().getSqlSegment().contains("knowledge_base_id"));
        verify(knowledgeBaseMapper).deleteById(1L);
    }

    @Test
    @DisplayName("delete() 知识库不存在抛 KNOWLEDGE_BASE_NOT_FOUND")
    void delete_不存在抛异常() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
        verify(knowledgeFileMapper, never()).delete(any());
        verify(knowledgeBaseMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("getById() 正常返回并映射 toDTO")
    void getById_正常返回() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(entity(1L, "kb", CommonStatus.DISABLED));

        KnowledgeBaseDTO result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("kb", result.getName());
        assertEquals("desc-kb", result.getDescription());
        assertEquals(CommonStatus.DISABLED, result.getStatus());
    }

    @Test
    @DisplayName("getById() 不存在抛 KNOWLEDGE_BASE_NOT_FOUND")
    void getById_不存在抛异常() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(1L));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("toggleStatus() 更新状态")
    void toggleStatus_更新状态() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(entity(1L, "kb", CommonStatus.ENABLED));

        KnowledgeBaseDTO result = service.toggleStatus(1L, CommonStatus.DISABLED);

        verify(knowledgeBaseMapper).updateById(any(KnowledgeBase.class));
        assertEquals(CommonStatus.DISABLED, result.getStatus());
    }

    @Test
    @DisplayName("toggleStatus() 不存在抛 KNOWLEDGE_BASE_NOT_FOUND")
    void toggleStatus_不存在抛异常() {
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleStatus(1L, CommonStatus.DISABLED));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("list() name/status 过滤")
    void list_过滤条件() {
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of(entity(1L, "kb", CommonStatus.ENABLED)));

        List<KnowledgeBaseDTO> result = service.list("kb", CommonStatus.ENABLED);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeBase>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeBaseMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql != null && sql.contains("name"));
        assertTrue(sql != null && sql.contains("status"));
        assertEquals(1, result.size());
        assertEquals("kb", result.get(0).getName());
    }

    @Test
    @DisplayName("list() 全 null 无过滤条件")
    void list_全null() {
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of());

        service.list(null, null);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeBase>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeBaseMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertFalse(sql != null && sql.contains("WHERE"));
    }
}
