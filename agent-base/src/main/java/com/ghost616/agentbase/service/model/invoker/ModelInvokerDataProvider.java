package com.ghost616.agentbase.service.model.invoker;

/**
 * 模型调用器数据提供者接口，封装不同平台 Invoker 的创建逻辑�? */
public interface ModelInvokerDataProvider {

    /**
     * 根据参数创建对应的平台调用器�?     *
     * @param platformType 平台类型
     * @param apiKey       API Key
     * @param baseUrl      基础 URL
     * @param modelName    模型名称
     * @param temperature  温度参数
     * @param maxTokens    最�?Token �?     * @return 平台对应�?ModelInvoker
     */
    ModelInvoker createInvoker(String platformType, String apiKey, String baseUrl,
                               String modelName, Double temperature, Integer maxTokens);
}
