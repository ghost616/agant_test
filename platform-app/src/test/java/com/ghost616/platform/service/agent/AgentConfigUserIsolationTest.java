package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.SkillConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AgentConfigServiceImpl 智能体配置数据用户隔离（user_id）专项测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>create() 从 {@link UserContext} 获取当前 userId 并填充到 entity.user_id；</li>
 *   <li>list() 仅按当前 userId 过滤，且随线程上下文切换而不混入其他用户过滤值；</li>
 *   <li>checkNameDuplicate() 按用户维度判断名称唯一性（不同用户可同名）；</li>
 *   <li>currentUserId() 在 UserContext 为空或会话用户为 null 时抛出
 *       {@link ErrorCode#USER_NOT_LOGIN}。</li>
 * </ul>
 *
 * <p>与 {@code ModelConfigServiceImplListFilterTest} 一致，通过
 * {@code TableInfoHelper.initTableInfo} 初始化实体表信息，使
 * {@link LambdaQueryWrapper#getSqlSegment()} 可解析 lambda 列名。</p>
 */
@ExtendWith(MockitoExtension.class)
class AgentConfigUserIsolationTest {

    /** 测试用户 A ID。 */
    private static final Long USER_A_ID = 100L;
    /** 测试用户 B ID。 */
    private static final Long USER_B_ID = 200L;

    @Mock
    private AgentConfigMapper agentConfigMapper;
    @Mock
    private AgentToolMapper agentToolMapper;
    @Mock
    private AgentSkillMapper agentSkillMapper;
    @Mock
    private SkillConfigMapper skillConfigMapper;
    @Mock
    private AgentKnowledgeBaseService agentKnowledgeBaseService;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Captor
    private ArgumentCaptor<AgentConfig> agentConfigCaptor;
    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<AgentConfig>> agentConfigWrapperCaptor;

    private AgentConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AgentConfig.class);
        service = new AgentConfigServiceImpl(agentConfigMapper, agentToolMapper,
                agentSkillMapper, skillConfigMapper, agentKnowledgeBaseService, knowledgeBaseMapper);
        login(USER_A_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** 以指定用户 ID 写入当前线程用户会话。 */
    private void login(Long userId) {
        User user = new User();
        user.setId(userId);
        UserContext.set(new UserSession("session-" + userId, user, System.currentTimeMillis()));
    }

    private AgentCreateRequest createRequest(String name) {
        return AgentCreateRequest.builder().name(name).build();
    }

    /** 使 toDTO 路径的关联查询全部返回空，避免触碰真实 DB。 */
    private void mockToDTOEmpty() {
        when(agentToolMapper.selectList(any())).thenReturn(List.of());
        when(agentSkillMapper.selectList(any())).thenReturn(List.of());
        when(agentKnowledgeBaseService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    /** 模拟 insert 后回填主键。 */
    private void mockInsertSetsId() {
        doAnswer(invocation -> {
            AgentConfig arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        }).when(agentConfigMapper).insert(any(AgentConfig.class));
    }

    @Nested
    @DisplayName("create 用户归属")
    class CreateUserOwnershipTest {

        @Test
        @DisplayName("create() 从 UserContext 填充 user_id 字段")
        void create_shouldFillUserIdFromUserContext() {
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);
            mockInsertSetsId();
            mockToDTOEmpty();

            service.create(createRequest("agent-a"));

            verify(agentConfigMapper).insert(agentConfigCaptor.capture());
            assertEquals(USER_A_ID, agentConfigCaptor.getValue().getUserId(),
                    "create 应把当前登录用户 ID 填充到 user_id 字段");
        }

        @Test
        @DisplayName("UserContext 为空时 create() 抛出 USER_NOT_LOGIN 且不执行插入")
        void create_withoutUserContext_shouldThrowUserNotLogin() {
            UserContext.clear();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(createRequest("no-login")));

            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(agentConfigMapper, never()).insert(any(AgentConfig.class));
            verify(agentConfigMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("会话用户为 null 时 create() 抛出 USER_NOT_LOGIN 且不执行插入")
        void create_sessionUserNull_shouldThrowUserNotLogin() {
            UserContext.set(new UserSession("session-null-user", null, System.currentTimeMillis()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(createRequest("no-user")));

            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(agentConfigMapper, never()).insert(any(AgentConfig.class));
        }
    }

    @Nested
    @DisplayName("list 数据隔离")
    class ListUserIsolationTest {

        @Test
        @DisplayName("list() 仅按当前 userId 过滤")
        void list_shouldFilterByCurrentUserId() {
            when(agentConfigMapper.selectList(any())).thenReturn(List.of());

            service.list(null, null);

            verify(agentConfigMapper).selectList(agentConfigWrapperCaptor.capture());
            LambdaQueryWrapper<AgentConfig> wrapper = agentConfigWrapperCaptor.getValue();
            String sql = wrapper.getSqlSegment();
            assertNotNull(sql);
            assertTrue(sql.contains("user_id"), "list 应包含 user_id 过滤条件, 实际: " + sql);
            assertTrue(wrapper.getParamNameValuePairs().containsValue(USER_A_ID),
                    "user_id 过滤值应为当前登录用户 ID");
        }

        @Test
        @DisplayName("list() 使用当前线程上下文用户 ID（换用户后不混入旧用户过滤值）")
        void list_shouldUseUserFromCurrentContext() {
            when(agentConfigMapper.selectList(any())).thenReturn(List.of());

            login(USER_B_ID);
            service.list(null, null);

            verify(agentConfigMapper).selectList(agentConfigWrapperCaptor.capture());
            LambdaQueryWrapper<AgentConfig> wrapper = agentConfigWrapperCaptor.getValue();
            assertTrue(wrapper.getParamNameValuePairs().containsValue(USER_B_ID),
                    "list 应使用当前线程上下文的用户 ID 过滤");
            assertFalse(wrapper.getParamNameValuePairs().containsValue(USER_A_ID),
                    "不应混入其他用户的 user_id 过滤值");
        }

        @Test
        @DisplayName("list() name/status 为 null 时仅保留 user_id 过滤条件")
        void list_allNull_onlyUserIdCondition() {
            when(agentConfigMapper.selectList(any())).thenReturn(List.of());

            service.list(null, null);

            verify(agentConfigMapper).selectList(agentConfigWrapperCaptor.capture());
            String sql = agentConfigWrapperCaptor.getValue().getSqlSegment();
            assertNotNull(sql);
            assertTrue(sql.contains("user_id"), "应保留 user_id 过滤条件, 实际: " + sql);
            assertFalse(sql.contains("name"), "全 null 时不应有 name 条件, 实际: " + sql);
            assertFalse(sql.contains("status"), "全 null 时不应有 status 条件, 实际: " + sql);
        }

        @Test
        @DisplayName("list() name/status 非空时仍保留 user_id 过滤且追加条件")
        void list_withNameAndStatus_keepsUserIdAndAppendsConditions() {
            when(agentConfigMapper.selectList(any())).thenReturn(List.of());

            service.list("my-agent", CommonStatus.ENABLED);

            verify(agentConfigMapper).selectList(agentConfigWrapperCaptor.capture());
            String sql = agentConfigWrapperCaptor.getValue().getSqlSegment();
            assertNotNull(sql);
            assertTrue(sql.contains("user_id"), "应始终保留 user_id 过滤, 实际: " + sql);
            assertTrue(sql.contains("name"), "应包含 name 过滤条件, 实际: " + sql);
            assertTrue(sql.contains("status"), "应包含 status 过滤条件, 实际: " + sql);
        }

        @Test
        @DisplayName("UserContext 为空时 list() 抛出 USER_NOT_LOGIN 且不执行查询")
        void list_withoutUserContext_shouldThrowUserNotLogin() {
            UserContext.clear();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.list(null, null));

            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(agentConfigMapper, never()).selectList(any());
        }
    }

    @Nested
    @DisplayName("checkNameDuplicate 用户维度唯一性")
    class NameDuplicatePerUserTest {

        @Test
        @DisplayName("同一用户重名抛 AGENT_ALREADY_EXISTS；不同用户可同名")
        void duplicateCheck_shouldBeScopedPerUser() {
            // selectCount 按 wrapper 中的 user_id 参数区分：用户 A 已有同名记录，用户 B 无
            when(agentConfigMapper.selectCount(any())).thenAnswer(invocation -> {
                LambdaQueryWrapper<AgentConfig> wrapper = invocation.getArgument(0);
                if (wrapper.getParamNameValuePairs().containsValue(USER_A_ID)) {
                    return 1L;
                }
                return 0L;
            });
            mockInsertSetsId();
            mockToDTOEmpty();

            // 用户 A 创建 "same-name" → 与自己的重名 → 拒绝
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(createRequest("same-name")));
            assertEquals(ErrorCode.AGENT_ALREADY_EXISTS, ex.getErrorCode());

            // 用户 B 创建同名 → 按用户维度不冲突 → 成功
            login(USER_B_ID);
            AgentConfigDTO dto = service.create(createRequest("same-name"));
            assertNotNull(dto);

            // 两次 selectCount 均按各自用户 ID 过滤
            verify(agentConfigMapper, times(2)).selectCount(any());
            verify(agentConfigMapper).insert(agentConfigCaptor.capture());
            assertEquals(USER_B_ID, agentConfigCaptor.getValue().getUserId(),
                    "用户 B 创建的记录应归属用户 B");
        }

        @Test
        @DisplayName("update() 重名校验按用户维度过滤且排除自身 excludeId")
        void update_duplicateCheck_shouldBeScopedPerUser() {
            AgentConfig entity = new AgentConfig();
            entity.setId(1L);
            entity.setName("old-name");
            entity.setStatus(CommonStatus.ENABLED);
            when(agentConfigMapper.selectById(1L)).thenReturn(entity);
            when(agentConfigMapper.selectCount(any())).thenReturn(0L);
            mockToDTOEmpty();

            AgentUpdateRequest req = AgentUpdateRequest.builder().name("new-name").build();
            service.update(1L, req);

            verify(agentConfigMapper).updateById(agentConfigCaptor.capture());
            assertEquals("new-name", agentConfigCaptor.getValue().getName());
        }

        @Test
        @DisplayName("update() 校验到同用户重名时抛 AGENT_ALREADY_EXISTS")
        void update_duplicateName_shouldThrow() {
            AgentConfig entity = new AgentConfig();
            entity.setId(1L);
            entity.setName("old-name");
            entity.setStatus(CommonStatus.ENABLED);
            when(agentConfigMapper.selectById(1L)).thenReturn(entity);
            when(agentConfigMapper.selectCount(any())).thenReturn(1L);

            AgentUpdateRequest req = AgentUpdateRequest.builder().name("dup-name").build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, req));
            assertEquals(ErrorCode.AGENT_ALREADY_EXISTS, ex.getErrorCode());
            verify(agentConfigMapper, never()).updateById(any(AgentConfig.class));
        }
    }
}