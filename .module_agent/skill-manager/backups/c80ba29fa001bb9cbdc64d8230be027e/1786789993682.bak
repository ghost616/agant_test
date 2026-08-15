package com.ghost616.platform.service.skill;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.entity.SkillConfig;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.repository.SkillConfigMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.repository.ToolConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillConfigServiceImplTest {

    @Mock
    private SkillConfigMapper skillConfigMapper;
    @Mock
    private SkillToolMapper skillToolMapper;
    @Mock
    private ToolConfigMapper toolConfigMapper;

    private SkillConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SkillConfigServiceImpl(skillConfigMapper, skillToolMapper, toolConfigMapper);
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
    void list_shouldReturnDTOs() {
        SkillConfig sc1 = createSkill(1L, "skill_1");
        SkillConfig sc2 = createSkill(2L, "skill_2");
        when(skillConfigMapper.selectList(any())).thenReturn(List.of(sc1, sc2));
        when(skillToolMapper.selectList(any())).thenReturn(List.of());

        List<SkillConfigDTO> dtos = service.list(null, null);

        assertEquals(2, dtos.size());
        assertNull(dtos.get(0).getSessionAuth());
        assertNull(dtos.get(1).getSessionAuth());
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
}
