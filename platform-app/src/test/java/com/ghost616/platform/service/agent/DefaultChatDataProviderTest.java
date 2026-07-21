package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultChatDataProviderTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private ApplicationContext applicationContext;

    private DefaultChatDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultChatDataProvider(modelConfigMapper, sessionMapper, applicationContext);
    }

    @Test
    void getModelConfig_实体存在_返回ModelConfigData() {
        ModelConfig entity = new ModelConfig();
        entity.setId(1L);
        entity.setApiKey("sk-test");
        entity.setBaseUrl("https://test.com");
        entity.setModelName("gpt-4");
        entity.setTemperature(0.7);
        entity.setMaxTokens(4096);
        entity.setPlatformType(PlatformType.OPENAI);
        when(modelConfigMapper.selectById(1L)).thenReturn(entity);

        ModelConfigData result = provider.getModelConfig(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("sk-test", result.apiKey());
        assertEquals("https://test.com", result.baseUrl());
        assertEquals("gpt-4", result.modelName());
        assertEquals(0.7, result.temperature());
        assertEquals(4096, result.maxTokens());
        assertEquals("OPENAI", result.platformType());
    }

    @Test
    void getModelConfig_实体为null_返回null() {
        when(modelConfigMapper.selectById(99L)).thenReturn(null);

        ModelConfigData result = provider.getModelConfig(99L);

        assertNull(result);
    }

    @Test
    void getModelConfig_platformType为null_platformType返回null() {
        ModelConfig entity = new ModelConfig();
        entity.setId(2L);
        entity.setApiKey("sk-another");
        entity.setBaseUrl("https://another.com");
        entity.setModelName("claude-3");
        entity.setTemperature(1.0);
        entity.setMaxTokens(8192);
        entity.setPlatformType(null);
        when(modelConfigMapper.selectById(2L)).thenReturn(entity);

        ModelConfigData result = provider.getModelConfig(2L);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertNull(result.platformType());
    }

    @Test
    void updateSessionModelId_会话存在_更新modelId() {
        Session session = new Session();
        session.setId(1L);
        session.setModelId(10L);
        session.setAgentId(100L);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        provider.updateSessionModelId(1L, 99L);

        assertEquals(99L, session.getModelId());
        verify(sessionMapper).updateById((Session) session);
    }

    @Test
    void updateSessionModelId_会话为null_不执行更新() {
        when(sessionMapper.selectById(999L)).thenReturn(null);

        provider.updateSessionModelId(999L, 99L);

        verify(sessionMapper, never()).updateById(any(Session.class));
    }

    @Test
    void getHooks_返回所有HookInvokerBean() {
        HookInvoker hook1 = mock(HookInvoker.class);
        HookInvoker hook2 = mock(HookInvoker.class);
        when(applicationContext.getBeansOfType(HookInvoker.class))
                .thenReturn(Map.of("hook1", hook1, "hook2", hook2));

        List<HookInvoker> hooks = provider.getHooks();

        assertEquals(2, hooks.size());
        assertTrue(hooks.contains(hook1));
        assertTrue(hooks.contains(hook2));
    }

    @Test
    void getHooks_无HookInvokerBean_返回空列表() {
        when(applicationContext.getBeansOfType(HookInvoker.class))
                .thenReturn(Map.of());

        List<HookInvoker> hooks = provider.getHooks();

        assertNotNull(hooks);
        assertTrue(hooks.isEmpty());
    }
}
