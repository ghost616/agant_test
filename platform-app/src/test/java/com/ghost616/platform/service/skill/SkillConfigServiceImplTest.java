package com.ghost616.platform.service.skill;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.entity.SkillConfig;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.SkillConfigMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.repository.ToolConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillConfigServiceImplTest {

    private static final Long CURRENT_USER_ID = 42L;

    @Mock
    private SkillConfigMapper skillConfigMapper;
    @Mock
    private SkillToolMapper skillToolMapper;
    @Mock
    private ToolConfigMapper toolConfigMapper;

    private SkillConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis-Plus 表元数据缓存，使 LambdaQueryWrapper 的 SQL 渲染（列名解析/参数填充）可用
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SkillConfig.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SkillTool.class);

        service = new SkillConfigServiceImpl(skillConfigMapper, skillToolMapper, toolConfigMapper);
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserSession session = new UserSession("session-1", user, System.currentTimeMillis());
        UserContext.set(session);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private SkillConfig createSkill(Long id, String name) {
        SkillConfig sc = new SkillConfig();
        sc.setId(id);
        sc.setName(name);
        sc.setDescription("desc");
        sc.setPrompt("prompt");
        sc.setStatus(CommonStatus.ENABLED);
        sc.setCreateTime(LocalDateTime.now());
        sc.setUpdateTime(LocalDateTime.now());
        return sc;
    }

    @Test
    void getById_shouldReturnDTO() {
        SkillConfig entity = createSkill(1L, "test_skill");
        when(skillConfigMapper.selectById(1L)).thenReturn(entity);
        when(skillToolMapper.selectList(any())).thenReturn(List.of());

        SkillConfigDTO dto = service.getById(1L);

        assertNotNull(dto);
        assertEquals("test_skill", dto.getName());
        assertNull(dto.getSessionAuth());
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_shouldReturnDTOs() {
        SkillConfig sc1 = createSkill(1L, "skill_1");
        SkillConfig sc2 = createSkill(2L, "skill_2");
        when(skillConfigMapper.selectList(any())).thenReturn(List.of(sc1, sc2));
        when(skillToolMapper.selectList(any())).thenReturn(List.of());

        List<SkillConfigDTO> dtos = service.list(null, null);

        assertEquals(2, dtos.size());
        assertNull(dtos.get(0).getSessionAuth());
        assertNull(dtos.get(1).getSessionAuth());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillConfig>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(skillConfigMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<SkillConfig> captured = wrapperCaptor.getValue();
        captured.getExpression().getSqlSegment(); // 渲染 SQL，触发 paramNameValuePairs 填充
        assertTrue(captured.getParamNameValuePairs().containsValue(CURRENT_USER_ID));
    }

    @Test
    void create_newSkill_shouldReturnDTO() {
        doAnswer(inv -> {
            SkillConfig arg = inv.getArgument(0);
            arg.setId(100L);
            arg.setCreateTime(LocalDateTime.now());
            arg.setUpdateTime(LocalDateTime.now());
            return null;
        }).when(skillConfigMapper).insert(any(SkillConfig.class));

        com.ghost616.platform.dto.skill.SkillCreateRequest request =
                com.ghost616.platform.dto.skill.SkillCreateRequest.builder()
                        .name("new_skill")
                        .description("new desc")
                        .prompt("new prompt")
                        .build();

        SkillConfigDTO dto = service.create(request);

        assertNotNull(dto);
        assertNull(dto.getSessionAuth());
        assertEquals("new_skill", dto.getName());

        ArgumentCaptor<SkillConfig> entityCaptor = ArgumentCaptor.forClass(SkillConfig.class);
        verify(skillConfigMapper).insert(entityCaptor.capture());
        assertEquals(CURRENT_USER_ID, entityCaptor.getValue().getUserId());
    }

    @Test
    void toggleStatus_shouldReturnDTO() {
        SkillConfig entity = createSkill(5L, "toggled_skill");
        when(skillConfigMapper.selectById(5L)).thenReturn(entity);
        when(skillToolMapper.selectList(any())).thenReturn(List.of());

        SkillConfigDTO dto = service.toggleStatus(5L, CommonStatus.DISABLED);

        assertNull(dto.getSessionAuth());
        assertEquals(CommonStatus.DISABLED, dto.getStatus());
    }

    @Test
    void list_whenNotLoggedIn_shouldThrowUserNotLogin() {
        UserContext.clear();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(null, null));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(skillConfigMapper, never()).selectList(any());
    }

    @Test
    void create_whenNotLoggedIn_shouldThrowUserNotLogin() {
        UserContext.clear();
        com.ghost616.platform.dto.skill.SkillCreateRequest request =
                com.ghost616.platform.dto.skill.SkillCreateRequest.builder()
                        .name("new_skill")
                        .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(skillConfigMapper, never()).insert(any(SkillConfig.class));
    }
}
