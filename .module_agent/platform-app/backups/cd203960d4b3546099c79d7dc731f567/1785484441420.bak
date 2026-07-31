package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class DefaultChatDataProvider implements ChatDataProvider {

    private final ModelConfigMapper modelConfigMapper;
    private final SessionMapper sessionMapper;
    private final ApplicationContext applicationContext;

    @Override
    public ModelConfigData getModelConfig(String modelId) {
        Long id = IdConverter.parse(modelId);
        if (id == null) {
            return null;
        }
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return new ModelConfigData(
                IdConverter.toString(entity.getId()),
                entity.getApiKey(),
                entity.getBaseUrl(),
                entity.getModelName(),
                entity.getTemperature(),
                entity.getMaxTokens(),
                entity.getPlatformType() != null ? entity.getPlatformType().name() : null
        );
    }

    @Override
    public void updateSessionModelId(String sessionId, String modelId) {
        Long sid = IdConverter.parse(sessionId);
        Long mid = IdConverter.parse(modelId);
        Session session = sessionMapper.selectById(sid);
        if (session != null) {
            session.setModelId(mid);
            sessionMapper.updateById(session);
        }
    }

    @Override
    public List<HookInvoker> getHooks() {
        Map<String, HookInvoker> map = applicationContext.getBeansOfType(HookInvoker.class);
        return new ArrayList<>(map.values());
    }

    @Override
    public List<HookInvoker> getHooks(String sessionId) {
        return List.of();
    }
}
