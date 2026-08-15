package com.ghost616.platform.service.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ModelType;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.repository.ModelConfigMapper;
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
class ModelConfigServiceImplListFilterTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;

    private ModelConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ModelConfig.class);
        service = new ModelConfigServiceImpl(modelConfigMapper, modelInvokerManager);
    }

    private ModelConfig entity(Long id, ModelType modelType, String name, PlatformType platformType, CommonStatus status) {
        ModelConfig e = new ModelConfig();
        e.setId(id);
        e.setModelType(modelType);
        e.setName(name);
        e.setPlatformType(platformType);
        e.setStatus(status);
        return e;
    }

    @Test
    @DisplayName("list() modelType 非空时应追加 model_type eq 过滤条件")
    void list_modelType非空应过滤() {
        when(modelConfigMapper.selectList(any())).thenReturn(List.of(entity(1L, ModelType.EMBEDDINGS, "m1", PlatformType.OPENAI, CommonStatus.ENABLED)));

        List<ModelConfigDTO> result = service.list(null, null, null, ModelType.EMBEDDINGS);

        ArgumentCaptor<LambdaQueryWrapper<ModelConfig>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(modelConfigMapper).selectList(captor.capture());
        LambdaQueryWrapper<ModelConfig> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertNotNull(sql);
        assertTrue(sql.contains("model_type"), "应包含 model_type 过滤条件, 实际: " + sql);
        assertEquals(1, result.size());
        assertEquals(ModelType.EMBEDDINGS, result.get(0).getModelType());
    }

    @Test
    @DisplayName("list() modelType 为 null 时不添加 model_type 过滤条件")
    void list_modelType为null不添加过滤() {
        when(modelConfigMapper.selectList(any())).thenReturn(List.of(entity(1L, ModelType.LLM, "m1", PlatformType.OPENAI, CommonStatus.ENABLED)));

        service.list(null, null, null, null);

        ArgumentCaptor<LambdaQueryWrapper<ModelConfig>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(modelConfigMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertFalse(sql != null && sql.contains("model_type"), "modelType 为 null 时不应包含 model_type 过滤条件, 实际: " + sql);
    }

    @Test
    @DisplayName("list() 组合条件 name/platformType/status/modelType 应同时生效")
    void list_组合条件同时生效() {
        when(modelConfigMapper.selectList(any())).thenReturn(List.of(entity(2L, ModelType.LLM, "gpt", PlatformType.OPENAI, CommonStatus.ENABLED)));

        service.list("gpt", PlatformType.OPENAI, CommonStatus.ENABLED, ModelType.LLM);

        ArgumentCaptor<LambdaQueryWrapper<ModelConfig>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(modelConfigMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertNotNull(sql);
        assertTrue(sql.contains("name"), "应包含 name 过滤, 实际: " + sql);
        assertTrue(sql.contains("platform_type"), "应包含 platform_type 过滤, 实际: " + sql);
        assertTrue(sql.contains("status"), "应包含 status 过滤, 实际: " + sql);
        assertTrue(sql.contains("model_type"), "应包含 model_type 过滤, 实际: " + sql);
    }

    @Test
    @DisplayName("list() 全部参数为 null 时无过滤条件")
    void list_全null无过滤() {
        when(modelConfigMapper.selectList(any())).thenReturn(List.of());

        service.list(null, null, null, null);

        ArgumentCaptor<LambdaQueryWrapper<ModelConfig>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(modelConfigMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertFalse(sql != null && sql.contains("WHERE"), "全 null 时不应有过滤条件, 实际: " + sql);
        assertFalse(sql != null && sql.contains("model_type"), "全 null 时不应有 model_type 条件, 实际: " + sql);
    }
}
