package com.ghost616.agentbase.service.model.invoker;

import com.ghost616.agentbase.dto.model.ModelConfigData;

public interface ModelInvokerFactory {
    ModelInvoker createInvoker(ModelConfigData config);
}
