package com.ghost616.agentinteg.tool;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrowserToolInvokerTest {

    @Mock
    private BrowserToolCallback callback;

    @Mock
    private AgentExecutionContext ctx;

    private ToolConfigDTO toolConfig;
    private BrowserToolInvoker invoker;

    @BeforeEach
    void setUp() {
        toolConfig = ToolConfigDTO.builder()
                .id(100L)
                .name("browser_tool")
                .build();
        invoker = new BrowserToolInvoker(toolConfig, callback);
    }

    @Test
    void 构造参数正确注入() {
        assertNotNull(invoker);
    }

    @Test
    void execute_从ctx获取sessionId并传递给callback() {
        when(ctx.getSessionId()).thenReturn(42L);
        when(callback.execute("42", "100", "browser_tool", "{\"url\":\"https://example.com\"}"))
                .thenReturn("result_data");

        String result = invoker.execute(ctx, "{\"url\":\"https://example.com\"}");

        assertEquals("result_data", result);
        verify(ctx).getSessionId();
        verify(callback).execute("42", "100", "browser_tool", "{\"url\":\"https://example.com\"}");
    }

    @Test
    void execute_从toolConfig获取id和name传递给callback() {
        ToolConfigDTO customConfig = ToolConfigDTO.builder()
                .id(200L)
                .name("custom_tool")
                .build();
        BrowserToolInvoker customInvoker = new BrowserToolInvoker(customConfig, callback);

        when(ctx.getSessionId()).thenReturn(1L);
        when(callback.execute("1", "200", "custom_tool", "{}")).thenReturn("ok");

        String result = customInvoker.execute(ctx, "{}");

        assertEquals("ok", result);
    }

    @Test
    void execute_callback抛出异常_返回JSON错误信息() {
        when(ctx.getSessionId()).thenReturn(1L);
        when(callback.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("浏览器执行失败"));

        String result = invoker.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("浏览器执行失败"));
    }

    @Test
    void execute_callback抛出异常_内部序列化也失败时返回简单错误JSON() {
        when(ctx.getSessionId()).thenReturn(1L);
        when(callback.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("普通错误"));

        String result = invoker.execute(ctx, "{}");

        assertTrue(result.contains("error"));
        assertTrue(result.contains("普通错误"));
    }

    @Test
    void loadJsContent_从classpath加载JS内容() {
        String content = invoker.loadJsContent();

        assertNotNull(content);
        assertTrue(content.contains("function execute"));
        assertTrue(content.contains("ToolManager"));
        assertTrue(content.contains("AgentExecutionContext"));
    }

    @Test
    void getJsContent_loadJsContent后返回缓存内容() {
        assertNull(invoker.getJsContent());

        String loaded = invoker.loadJsContent();
        String cached = invoker.getJsContent();

        assertNotNull(cached);
        assertEquals(loaded, cached);
    }

    @Test
    void loadJsContent_多次调用返回相同内容() {
        String first = invoker.loadJsContent();
        String second = invoker.loadJsContent();

        assertNotNull(first);
        assertSame(first, second);
    }
}
