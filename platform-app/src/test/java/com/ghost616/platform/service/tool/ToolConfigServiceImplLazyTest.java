package com.ghost616.platform.service.tool;

import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;

class ToolConfigServiceImplLazyTest {

    @Test
    void classShouldBeAnnotatedWithService() {
        assertTrue(ToolConfigServiceImpl.class.isAnnotationPresent(Service.class));
    }

    @Test
    void toolManagerConstructorParameterShouldHaveLazyAnnotation() {
        Constructor<?>[] constructors = ToolConfigServiceImpl.class.getConstructors();
        assertTrue(constructors.length > 0, "Should have at least one public constructor");

        Constructor<?> constructor = constructors[0];
        Parameter[] parameters = constructor.getParameters();

        Parameter toolManagerParam = null;
        for (Parameter param : parameters) {
            if (param.getType().equals(ToolManager.class)) {
                toolManagerParam = param;
                break;
            }
        }

        assertNotNull(toolManagerParam, "Constructor should have a ToolManager parameter");

        boolean hasLazy = false;
        for (Annotation annotation : toolManagerParam.getAnnotations()) {
            if (annotation.annotationType().equals(Lazy.class)) {
                hasLazy = true;
                break;
            }
        }
        assertTrue(hasLazy, "ToolManager constructor parameter should have @Lazy annotation");
    }

    @Test
    void constructorShouldHaveThreeParameters() {
        Constructor<?>[] constructors = ToolConfigServiceImpl.class.getConstructors();
        Constructor<?> constructor = constructors[0];
        assertEquals(3, constructor.getParameterCount(), "Constructor should have 3 parameters");
    }
}
