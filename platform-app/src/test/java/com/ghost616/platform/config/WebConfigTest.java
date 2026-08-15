package com.ghost616.platform.config;

import com.ghost616.platform.session.UserSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebConfig 单元测试（不使用 Mockito，沙箱环境禁子进程）。
 *
 * <p>覆盖：AuthInterceptor 注册（拦截 /api/**、排除 /api/auth/login）、CORS 配置保留。
 * Spring 6 中 InterceptorRegistry/CorsRegistry 的内部读取 API 为 protected 或缺失，
 * 故使用反射读取注册结构。</p>
 */
class WebConfigTest {

    private static Object getField(Object target, String name) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " in " + target.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void 注册AuthInterceptor拦截api排除login() throws Exception {
        AuthInterceptor authInterceptor = new AuthInterceptor(new UserSessionManager());
        WebConfig webConfig = new WebConfig(authInterceptor);
        InterceptorRegistry registry = new InterceptorRegistry();
        webConfig.addInterceptors(registry);

        List<InterceptorRegistration> registrations =
                (List<InterceptorRegistration>) getField(registry, "registrations");
        assertEquals(1, registrations.size(), "应注册 1 个拦截器");

        InterceptorRegistration registration = registrations.get(0);
        Object registered = getField(registration, "interceptor");
        assertSame(authInterceptor, registered, "注册的应为 AuthInterceptor 实例");

        List<String> includes = (List<String>) getField(registration, "includePatterns");
        assertTrue(includes.contains("/api/**"), "应拦截 /api/**，实际: " + includes);
        List<String> excludes = (List<String>) getField(registration, "excludePatterns");
        assertTrue(excludes.contains("/api/auth/login"), "应排除 /api/auth/login，实际: " + excludes);
    }

    static class ExposedCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configs() {
            return getCorsConfigurations();
        }
    }

    @Test
    void 保留CORS配置() {
        WebConfig webConfig = new WebConfig(new AuthInterceptor(new UserSessionManager()));
        ExposedCorsRegistry registry = new ExposedCorsRegistry();
        webConfig.addCorsMappings(registry);
        assertFalse(registry.configs().isEmpty(), "CORS 配置不应为空");
        assertTrue(registry.configs().containsKey("/**"), "应配置 /** 路径的 CORS");
    }
}