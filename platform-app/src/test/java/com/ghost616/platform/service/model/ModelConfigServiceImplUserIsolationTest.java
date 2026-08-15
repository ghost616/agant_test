package com.ghost616.platform.service.model;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.repository.ModelConfigMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录态正常流程测试：UserContext 存在有效用户时，
 * create() 应将 userId 填充为当前登录用户 ID，
 * list() 应按当前用户 ID 过滤查询（数据用户隔离）。
 *
 * <p>覆盖本次重构（删除私有 currentUserId()，改用
 * UserContextUtil.requireUserId()）后的登录态行为保持。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplUserIsolationTest {

    /** 测试用当前登录用户 ID。 */
    private static final Long CURRENT_USER_ID = 100L;

    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;

    private ModelConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ModelConfig.class);
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserContext.set(new UserSession("test-session", user, System.currentTimeMillis()));
        service = new ModelConfigServiceImpl(modelConfigMapper, modelInvokerManager);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("登录态下 create() 将 userId 填充为当前登录用户 ID")
    void create_登录态填充当前用户ID() {
        when(modelConfigMapper.selectCount(any())).thenReturn(0L);

        ModelCreateRequest request = ModelCreateRequest.builder()
                .name("test-model")
                .platformType(PlatformType.OPENAI)
                .apiKey("sk-test")
                .build();
        service.create(request);

        ArgumentCaptor<ModelConfig> captor = ArgumentCaptor.forClass(ModelConfig.class);
        verify(modelConfigMapper).insert(captor.capture());
        assertEquals(CURRENT_USER_ID, captor.getValue().getUserId(),
                "插入实体的 userId 应为当前登录用户 ID");
    }

    @Test
    @DisplayName("登录态下 list() 按当前用户 ID 过滤查询")
    void list_登录态按当前用户过滤() {
        ModelConfig entity = new ModelConfig();
        entity.setId(1L);
        entity.setUserId(CURRENT_USER_ID);
        entity.setName("test-model");
        when(modelConfigMapper.selectList(any())).thenReturn(List.of(entity));

        List<?> result = service.list(null, null, null, null);

        ArgumentCaptor<LambdaQueryWrapper<ModelConfig>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(modelConfigMapper).selectList(captor.capture());
        LambdaQueryWrapper<ModelConfig> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertNotNull(sql, "查询条件不应为空");
        assertTrue(sql.contains("user_id"), "应包含 user_id 过滤条件, 实际: " + sql);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(CURRENT_USER_ID),
                "user_id 过滤值应为当前登录用户 ID, 实际: " + wrapper.getParamNameValuePairs());
        assertEquals(1, result.size());
    }
}