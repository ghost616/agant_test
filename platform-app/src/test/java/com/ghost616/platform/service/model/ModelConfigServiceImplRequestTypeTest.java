package com.ghost616.platform.service.model;

import com.ghost616.agentbase.enums.RequestType;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.dto.model.ModelUpdateRequest;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplRequestTypeTest {

    /** 测试用当前登录用户 ID。 */
    private static final Long CURRENT_USER_ID = 100L;

    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;

    private ModelConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserContext.set(new UserSession("test-session", user, System.currentTimeMillis()));
        service = new ModelConfigServiceImpl(modelConfigMapper, modelInvokerManager);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ModelCreateRequest createRequest(String requestType) {
        return ModelCreateRequest.builder()
                .name("test-model")
                .platformType(PlatformType.OPENAI)
                .apiKey("sk-test")
                .requestType(requestType)
                .build();
    }

    @Test
    @DisplayName("create() 应将 requestType 持久化到实体并映射到 DTO")
    void create_应持久化并映射requestType() {
        when(modelConfigMapper.selectCount(any())).thenReturn(0L);

        ModelConfigDTO result = service.create(createRequest(RequestType.RESPONSES.getCode()));

        ArgumentCaptor<ModelConfig> captor = ArgumentCaptor.forClass(ModelConfig.class);
        verify(modelConfigMapper).insert(captor.capture());
        assertEquals(RequestType.RESPONSES.getCode(), captor.getValue().getRequestType(),
                "插入实体的 requestType 应为 responses");
        assertEquals(CURRENT_USER_ID, captor.getValue().getUserId(),
                "插入实体的 userId 应为当前登录用户 ID");
        assertEquals(RequestType.RESPONSES.getCode(), result.getRequestType(),
                "返回 DTO 的 requestType 应为 responses");
    }

    @Test
    @DisplayName("update() requestType 非空时应更新实体")
    void update_requestType非空应更新() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setRequestType("openai");
        when(modelConfigMapper.selectById(1L)).thenReturn(existing);

        ModelUpdateRequest request = ModelUpdateRequest.builder()
                .requestType(RequestType.RESPONSES.getCode())
                .build();
        ModelConfigDTO result = service.update(1L, request);

        verify(modelConfigMapper).updateById(existing);
        assertEquals(RequestType.RESPONSES.getCode(), existing.getRequestType(),
                "实体 requestType 应更新为 responses");
        assertEquals(RequestType.RESPONSES.getCode(), result.getRequestType());
        verify(modelInvokerManager).evict("1");
    }

    @Test
    @DisplayName("update() requestType 为 null 时应保留原值")
    void update_requestType为null应保留原值() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setRequestType("openai");
        when(modelConfigMapper.selectById(1L)).thenReturn(existing);

        ModelUpdateRequest request = ModelUpdateRequest.builder().build();
        service.update(1L, request);

        verify(modelConfigMapper).updateById(existing);
        assertEquals("openai", existing.getRequestType(),
                "requestType 为 null 时不应覆盖实体原值");
    }

    @Test
    @DisplayName("getById() 应通过 toDTO 返回 requestType")
    void getById_应映射requestType() {
        ModelConfig entity = new ModelConfig();
        entity.setId(1L);
        entity.setRequestType(RequestType.RESPONSES.getCode());
        when(modelConfigMapper.selectById(1L)).thenReturn(entity);

        ModelConfigDTO result = service.getById(1L);

        assertEquals(RequestType.RESPONSES.getCode(), result.getRequestType());
    }
}
