package com.ghost616.platform.service.agent.invoker;

import com.ghost616.platform.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaToolInvoker implements ToolInvoker {

    private final String implClassName;
    private final ToolInvoker delegate;

    public JavaToolInvoker(String implClassName) {
        this.implClassName = implClassName;
        this.delegate = loadInstance();
    }

    private ToolInvoker loadInstance() {
        try {
            Class<?> clazz = Class.forName(implClassName);
            return (ToolInvoker) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("加载工具实现类失败: {}", implClassName, e);
            throw new RuntimeException("加载工具实现类失败: " + implClassName, e);
        }
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        return delegate.execute(ctx, arguments);
    }
}
