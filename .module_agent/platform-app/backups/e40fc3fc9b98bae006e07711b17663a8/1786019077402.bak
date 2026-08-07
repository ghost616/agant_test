package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentSkillItem;
import com.ghost616.platform.dto.agent.AgentToolItem;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.AgentTool;
import com.ghost616.platform.entity.SkillConfig;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.SkillConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigServiceImplTest {

    @Mock private AgentConfigMapper agentConfigMapper;
    @Mock private AgentToolMapper agentToolMapper;
    @Mock private AgentSkillMapper agentSkillMapper;
    @Mock private SkillConfigMapper skillConfigMapper;

    @Captor private ArgumentCaptor<AgentTool> agentToolCaptor;
    @Captor private ArgumentCaptor<AgentSkill> agentSkillCaptor;

    private AgentConfigServiceImpl service;

    private final Long EXISTING_AGENT_ID = 1L;
    private final Long TOOL_ID = 100L;
    private final Long SKILL_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new AgentConfigServiceImpl(agentConfigMapper, agentToolMapper,
                agentSkillMapper, skillConfigMapper);
    }

    private AgentConfig createAgentEntity() {
        AgentConfig entity = new AgentConfig();
        entity.setId(EXISTING_AGENT_ID);
        entity.setName("test-agent");
        entity.setStatus(CommonStatus.ENABLED);
        return entity;
    }

    private void mockToDTOReturns(List<AgentTool> tools, List<AgentSkill> skills) {
        when(agentToolMapper.selectList(any())).thenReturn(tools);
        when(agentSkillMapper.selectList(any())).thenReturn(skills);
    }

    private void mockInsertSetsId() {
        doAnswer(invocation -> {
            AgentConfig arg = invocation.getArgument(0);
            arg.setId(EXISTING_AGENT_ID);
            return 1;
        }).when(agentConfigMapper).insert(any(AgentConfig.class));
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("tools 传入 sessionAuth 时正确写入")
        void toolsWithSessionAuth_writtenCorrectly() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(List.of(new AgentToolItem(TOOL_ID, SessionAuthType.PARENT)))
                    .skills(List.of())
                    .build();

            service.create(req);

            verify(agentToolMapper).insert(agentToolCaptor.capture());
            AgentTool inserted = agentToolCaptor.getValue();
            assertEquals(EXISTING_AGENT_ID, inserted.getAgentId());
            assertEquals(TOOL_ID, inserted.getToolId());
            assertEquals(SessionAuthType.PARENT, inserted.getSessionAuth());
        }

        @Test
        @DisplayName("tools 未传 sessionAuth 时默认使用 ALL")
        void toolsWithoutSessionAuth_defaultsToAll() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(List.of(new AgentToolItem(TOOL_ID, null)))
                    .skills(List.of())
                    .build();

            service.create(req);

            verify(agentToolMapper).insert(agentToolCaptor.capture());
            assertEquals(SessionAuthType.ALL, agentToolCaptor.getValue().getSessionAuth());
        }

        @Test
        @DisplayName("skills 传入 sessionAuth 时正确写入")
        void skillsWithSessionAuth_writtenCorrectly() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);
            when(skillConfigMapper.selectBatchIds(any())).thenReturn(List.of(new SkillConfig()));

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(List.of())
                    .skills(List.of(new AgentSkillItem(SKILL_ID, SessionAuthType.CHILD)))
                    .build();

            service.create(req);

            verify(agentSkillMapper).insert(agentSkillCaptor.capture());
            AgentSkill inserted = agentSkillCaptor.getValue();
            assertEquals(EXISTING_AGENT_ID, inserted.getAgentId());
            assertEquals(SKILL_ID, inserted.getSkillId());
            assertEquals(SessionAuthType.CHILD, inserted.getSessionAuth());
        }

        @Test
        @DisplayName("skills 未传 sessionAuth 时默认使用 ALL")
        void skillsWithoutSessionAuth_defaultsToAll() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);
            when(skillConfigMapper.selectBatchIds(any())).thenReturn(List.of(new SkillConfig()));

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(List.of())
                    .skills(List.of(new AgentSkillItem(SKILL_ID, null)))
                    .build();

            service.create(req);

            verify(agentSkillMapper).insert(agentSkillCaptor.capture());
            assertEquals(SessionAuthType.ALL, agentSkillCaptor.getValue().getSessionAuth());
        }

        @Test
        @DisplayName("空 tools 列表不插入关联记录")
        void emptyTools_skipsInsert() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(List.of())
                    .build();

            service.create(req);

            verify(agentToolMapper, never()).insert(any(AgentTool.class));
        }

        @Test
        @DisplayName("null tools 不插入关联记录")
        void nullTools_skipsInsert() {
            mockInsertSetsId();
            mockToDTOReturns(List.of(), List.of());
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .tools(null)
                    .build();

            service.create(req);

            verify(agentToolMapper, never()).insert(any(AgentTool.class));
        }

        @Test
        @DisplayName("重复名称抛出 BusinessException")
        void duplicateName_throwsException() {
            when(agentConfigMapper.selectCount(any())).thenReturn(1L);

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("dup-name")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
            assertEquals(ErrorCode.AGENT_ALREADY_EXISTS, ex.getErrorCode());
        }

        @Test
        @DisplayName("不存在的 skillId 抛出 BusinessException")
        void invalidSkillId_throwsException() {
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);
            when(skillConfigMapper.selectBatchIds(any())).thenReturn(List.of());

            AgentCreateRequest req = AgentCreateRequest.builder()
                    .name("new-agent")
                    .skills(List.of(new AgentSkillItem(999L, SessionAuthType.ALL)))
                    .build();

            assertThrows(BusinessException.class, () -> service.create(req));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("传入 tools，先删除旧关联再插入新关联，sessionAuth 正确写入")
        void updateTools_deletesOldAndInsertsNew() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .tools(List.of(new AgentToolItem(TOOL_ID, SessionAuthType.CHILD)))
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentToolMapper).delete(any());
            verify(agentToolMapper).insert(agentToolCaptor.capture());
            AgentTool inserted = agentToolCaptor.getValue();
            assertEquals(TOOL_ID, inserted.getToolId());
            assertEquals(SessionAuthType.CHILD, inserted.getSessionAuth());
        }

        @Test
        @DisplayName("tools 未传 sessionAuth 时默认使用 ALL")
        void updateToolsWithoutSessionAuth_defaultsToAll() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .tools(List.of(new AgentToolItem(TOOL_ID, null)))
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentToolMapper).insert(agentToolCaptor.capture());
            assertEquals(SessionAuthType.ALL, agentToolCaptor.getValue().getSessionAuth());
        }

        @Test
        @DisplayName("tools 为 null 时不处理关联表")
        void nullTools_doesNotTouchToolTable() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .tools(null)
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentToolMapper, never()).delete(any());
            verify(agentToolMapper, never()).insert(any(AgentTool.class));
        }

        @Test
        @DisplayName("空 tools 列表删除旧关联但不插入新关联")
        void emptyTools_deletesButNoInsert() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .tools(List.of())
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentToolMapper).delete(any());
            verify(agentToolMapper, never()).insert(any(AgentTool.class));
        }

        @Test
        @DisplayName("传入 skills，先删除旧关联再插入新关联，sessionAuth 正确写入")
        void updateSkills_deletesOldAndInsertsNew() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());
            when(skillConfigMapper.selectBatchIds(any())).thenReturn(List.of(new SkillConfig()));

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .skills(List.of(new AgentSkillItem(SKILL_ID, SessionAuthType.PARENT)))
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentSkillMapper).delete(any());
            verify(agentSkillMapper).insert(agentSkillCaptor.capture());
            assertEquals(SKILL_ID, agentSkillCaptor.getValue().getSkillId());
            assertEquals(SessionAuthType.PARENT, agentSkillCaptor.getValue().getSessionAuth());
        }

        @Test
        @DisplayName("skills 为 null 时不处理关联表")
        void nullSkills_doesNotTouchSkillTable() {
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(createAgentEntity());
            mockToDTOReturns(List.of(), List.of());

            AgentUpdateRequest req = AgentUpdateRequest.builder()
                    .name("updated")
                    .skills(null)
                    .build();

            service.update(EXISTING_AGENT_ID, req);

            verify(agentSkillMapper, never()).delete(any());
            verify(agentSkillMapper, never()).insert(any(AgentSkill.class));
        }

        @Test
        @DisplayName("不存在的智能体抛出 BusinessException")
        void agentNotFound_throwsException() {
            when(agentConfigMapper.selectById(999L)).thenReturn(null);

            AgentUpdateRequest req = AgentUpdateRequest.builder().name("any").build();

            assertThrows(BusinessException.class, () -> service.update(999L, req));
        }
    }

    @Nested
    @DisplayName("toDTO")
    class ToDTOTest {

        @Test
        @DisplayName("getById 返回值中 tools 列表包含 toolId 和 sessionAuth")
        void getById_containsToolIdAndSessionAuth() {
            AgentConfig entity = createAgentEntity();
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(entity);

            AgentTool agentTool = new AgentTool();
            agentTool.setToolId(TOOL_ID);
            agentTool.setSessionAuth(SessionAuthType.CHILD);
            mockToDTOReturns(List.of(agentTool), List.of());

            AgentConfigDTO dto = service.getById(EXISTING_AGENT_ID);

            assertEquals(1, dto.getTools().size());
            assertEquals(TOOL_ID, dto.getTools().get(0).toolId());
            assertEquals(SessionAuthType.CHILD, dto.getTools().get(0).sessionAuth());
        }

        @Test
        @DisplayName("getById 返回值中 skills 列表包含 skillId 和 sessionAuth")
        void getById_containsSkillIdAndSessionAuth() {
            AgentConfig entity = createAgentEntity();
            when(agentConfigMapper.selectById(EXISTING_AGENT_ID)).thenReturn(entity);

            AgentSkill agentSkill = new AgentSkill();
            agentSkill.setSkillId(SKILL_ID);
            agentSkill.setSessionAuth(SessionAuthType.PARENT);
            mockToDTOReturns(List.of(), List.of(agentSkill));

            AgentConfigDTO dto = service.getById(EXISTING_AGENT_ID);

            assertEquals(1, dto.getSkills().size());
            assertEquals(SKILL_ID, dto.getSkills().get(0).skillId());
            assertEquals(SessionAuthType.PARENT, dto.getSkills().get(0).sessionAuth());
        }
    }
}
