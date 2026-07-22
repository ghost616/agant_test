package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
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
    public ModelConfigData getModelConfig(Long id) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return new ModelConfigData(
                entity.getId(),
                entity.getApiKey(),
                entity.getBaseUrl(),
                entity.getModelName(),
                entity.getTemperature(),
                entity.getMaxTokens(),
                entity.getPlatformType() != null ? entity.getPlatformType().name() : null
        );
    }

    @Override
    public void updateSessionModelId(Long id, Long modelId) {
        Session session = sessionMapper.selectById(id);
        if (session != null) {
            session.setModelId(modelId);
            sessionMapper.updateById(session);
        }
    }

    @Override
    public List<HookInvoker> getHooks() {
        Map<String, HookInvoker> map = applicationContext.getBeansOfType(HookInvoker.class);
        return new ArrayList<>(map.values());
    }
}
