package com.ghost616.platform.service.model;

import com.ghost616.agentbase.enums.ModelType;
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
class ModelConfigServiceImplModelTypeTest {

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

    @Test
    @DisplayName("create() 传入 modelType 时应持久化到实体并映射到 DTO")
    void create_modelType非空应持久化并映射() {
        when(modelConfigMapper.selectCount(any())).thenReturn(0L);

        ModelCreateRequest request = ModelCreateRequest.builder()
                .name("test-model")
                .platformType(PlatformType.SILICONFLOW)
                .apiKey("sk-test")
                .modelType(ModelType.EMBEDDINGS)
                .build();
        ModelConfigDTO result = service.create(request);

        ArgumentCaptor<ModelConfig> captor = ArgumentCaptor.forClass(ModelConfig.class);
        verify(modelConfigMapper).insert(captor.capture());
        assertEquals(ModelType.EMBEDDINGS, captor.getValue().getModelType(),
                "插入实体的 modelType 应为 EMBEDDINGS");
        assertEquals(CURRENT_USER_ID, captor.getValue().getUserId(),
                "插入实体的 userId 应为当前登录用户 ID");
        assertEquals(ModelType.EMBEDDINGS, result.getModelType(),
                "返回 DTO 的 modelType 应为 EMBEDDINGS");
    }

    @Test
    @DisplayName("create() modelType 为空时应默认 LLM")
    void create_modelType为空默认LLM() {
        when(modelConfigMapper.selectCount(any())).thenReturn(0L);

        ModelCreateRequest request = ModelCreateRequest.builder()
                .name("test-model")
                .platformType(PlatformType.OPENAI)
                .apiKey("sk-test")
                .build();
        ModelConfigDTO result = service.create(request);

        ArgumentCaptor<ModelConfig> captor = ArgumentCaptor.forClass(ModelConfig.class);
        verify(modelConfigMapper).insert(captor.capture());
        assertEquals(ModelType.LLM, captor.getValue().getModelType(),
                "插入实体 modelType 应为默认 LLM");
        assertEquals(ModelType.LLM, result.getModelType(),
                "返回 DTO modelType 应为默认 LLM");
    }

    @Test
    @DisplayName("update() modelType 非空时应更新实体")
    void update_modelType非空应更新() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setModelType(ModelType.LLM);
        when(modelConfigMapper.selectById(1L)).thenReturn(existing);

        ModelUpdateRequest request = ModelUpdateRequest.builder()
                .modelType(ModelType.EMBEDDINGS)
                .build();
        ModelConfigDTO result = service.update(1L, request);

        verify(modelConfigMapper).updateById(existing);
        assertEquals(ModelType.EMBEDDINGS, existing.getModelType(),
                "实体 modelType 应更新为 EMBEDDINGS");
        assertEquals(ModelType.EMBEDDINGS, result.getModelType());
        verify(modelInvokerManager).evict("1");
    }

    @Test
    @DisplayName("update() modelType 为 null 时应保留原值")
    void update_modelType为null应保留原值() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setModelType(ModelType.EMBEDDINGS);
        when(modelConfigMapper.selectById(1L)).thenReturn(existing);

        ModelUpdateRequest request = ModelUpdateRequest.builder().build();
        service.update(1L, request);

        verify(modelConfigMapper).updateById(existing);
        assertEquals(ModelType.EMBEDDINGS, existing.getModelType(),
                "modelType 为 null 时不应覆盖实体原值");
    }

    @Test
    @DisplayName("getById() 应通过 toDTO 返回 modelType")
    void getById_应映射modelType() {
        ModelConfig entity = new ModelConfig();
        entity.setId(1L);
        entity.setModelType(ModelType.EMBEDDINGS);
        when(modelConfigMapper.selectById(1L)).thenReturn(entity);

        ModelConfigDTO result = service.getById(1L);

        assertEquals(ModelType.EMBEDDINGS, result.getModelType());
    }
}
