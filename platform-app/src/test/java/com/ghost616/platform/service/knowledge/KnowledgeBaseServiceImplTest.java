package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
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

    private static final Long CURRENT_USER_ID = 42L;

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
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserSession session = new UserSession("session-1", user, System.currentTimeMillis());
        UserContext.set(session);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
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
    @DisplayName("create() 默认 status=ENABLED、设置 vectorModelId，esIndex 为空时自动生成")
    void create_默认ENABLED() {
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            KnowledgeBase e = invocation.getArgument(0);
            e.setId(7L);
            return 1;
        }).when(knowledgeBaseMapper).insert(any(KnowledgeBase.class));

        KnowledgeBaseCreateRequest request = KnowledgeBaseCreateRequest.builder()
                .name("kb-1")
                .description("知识库描述")
                .vectorModelId(5L)
                .build();
        KnowledgeBaseDTO result = service.create(request);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).insert((KnowledgeBase) captor.capture());
        KnowledgeBase saved = captor.getValue();
        assertEquals("kb-1", saved.getName());
        assertEquals("知识库描述", saved.getDescription());
        assertEquals(5L, saved.getVectorModelId());
        assertEquals(CommonStatus.ENABLED, saved.getStatus(), "create 默认状态应为 ENABLED");
        assertNotNull(saved.getEsIndex(), "create 应自动生成 esIndex");
        assertTrue(saved.getEsIndex().startsWith("agent_7_"), "esIndex 应以 agent_<id>_ 开头, 实际: " + saved.getEsIndex());
        verify(knowledgeBaseMapper).updateById((KnowledgeBase) saved);
        assertEquals(CommonStatus.ENABLED, result.getStatus());
        assertEquals("kb-1", result.getName());
        assertEquals(5L, result.getVectorModelId());
        assertEquals(saved.getEsIndex(), result.getEsIndex());
    }

    @Test
    @DisplayName("create() 传入 esIndex 时保留指定值，不重新生成")
    void create_指定esIndex() {
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);

        KnowledgeBaseCreateRequest request = KnowledgeBaseCreateRequest.builder()
                .name("kb-2")
                .vectorModelId(5L)
                .esIndex("agent_custom")
                .build();
        KnowledgeBaseDTO result = service.create(request);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).insert((KnowledgeBase) captor.capture());
        assertEquals("agent_custom", captor.getValue().getEsIndex());
        verify(knowledgeBaseMapper, never()).updateById(any(KnowledgeBase.class));
        assertEquals("agent_custom", result.getEsIndex());
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
    @DisplayName("update() 当 esIndex 为空时自动生成")
    void update_esIndex为空自动生成() {
        KnowledgeBase existing = entity(1L, "kb", CommonStatus.ENABLED);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(existing);

        KnowledgeBaseUpdateRequest request = KnowledgeBaseUpdateRequest.builder()
                .vectorModelId(9L)
                .build();
        KnowledgeBaseDTO result = service.update(1L, request);

        assertEquals(9L, existing.getVectorModelId());
        assertNotNull(existing.getEsIndex(), "esIndex 为空时应自动生成");
        assertTrue(existing.getEsIndex().startsWith("agent_1_"), "实际: " + existing.getEsIndex());
        verify(knowledgeBaseMapper).updateById((KnowledgeBase) existing);
        assertEquals(9L, result.getVectorModelId());
        assertEquals(existing.getEsIndex(), result.getEsIndex());
    }

    @Test
    @DisplayName("update() 传入 esIndex 时更新指定值")
    void update_更新esIndex() {
        KnowledgeBase existing = entity(1L, "kb", CommonStatus.ENABLED);
        existing.setEsIndex("old_index");
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(existing);

        KnowledgeBaseUpdateRequest request = KnowledgeBaseUpdateRequest.builder()
                .esIndex("new_index")
                .build();
        service.update(1L, request);

        assertEquals("new_index", existing.getEsIndex());
    }

    @Test
    @DisplayName("getById() 正常返回并映射 toDTO")
    void getById_正常返回() {
        KnowledgeBase kb = entity(1L, "kb", CommonStatus.DISABLED);
        kb.setVectorModelId(5L);
        kb.setEsIndex("agent_1_a1b2c3");
        kb.setRebuilding(true);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);

        KnowledgeBaseDTO result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("kb", result.getName());
        assertEquals("desc-kb", result.getDescription());
        assertEquals(CommonStatus.DISABLED, result.getStatus());
        assertEquals(5L, result.getVectorModelId());
        assertEquals("agent_1_a1b2c3", result.getEsIndex());
        assertEquals(Boolean.TRUE, result.getRebuilding());
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
    @DisplayName("list() 全 null 时仅保留 user_id 过滤条件")
    void list_全null() {
        when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of());

        service.list(null, null);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeBase>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeBaseMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql != null && sql.contains("user_id"), "全 null 时应保留 user_id 过滤条件, 实际: " + sql);
        assertFalse(sql != null && sql.contains("name"), "不应包含 name 条件: " + sql);
        assertFalse(sql != null && sql.contains("status"), "不应包含 status 条件: " + sql);
    }
}
