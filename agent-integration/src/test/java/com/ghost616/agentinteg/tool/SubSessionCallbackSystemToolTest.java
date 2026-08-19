package com.ghost616.agentinteg.tool;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.SubSessionCallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubSessionCallbackSystemToolTest {

    @Mock
    private SubSessionCallback callback;

    @Mock
    private AgentExecutionContext ctx;

    private SubSessionCallbackSystemTool tool;

    @BeforeEach
    void setUp() {
        tool = new SubSessionCallbackSystemTool(callback);
    }

    @Test
    void getToolName_返回callback_sub_session() {
        assertEquals("callback_sub_session", tool.getToolName());
    }

    @Test
    void getDescription_返回非空描述() {
        String desc = tool.getDescription();
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void getParameterSchema_包含必填参数sessionName和userMessage() {
        String schema = tool.getParameterSchema();
        assertNotNull(schema);
        assertTrue(schema.contains("\"sessionName\""));
        assertTrue(schema.contains("\"userMessage\""));
        assertTrue(schema.contains("\"required\""));
        assertTrue(schema.contains("\"sessionName\""));
    }

    @Test
    void execute_正常路径_返回消息内容() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "description": "测试描述",
                    "toolNames": ["tool1", "tool2"],
                    "skillNames": ["skill1"],
                    "userMessage": "hello"
                }
                """;

        ToolConfigDTO tool1 = ToolConfigDTO.builder().id("1").name("tool1").build();
        ToolConfigDTO tool2 = ToolConfigDTO.builder().id("2").name("tool2").build();
        SkillConfigDTO skill1 = SkillConfigDTO.builder().id("10").name("skill1").build();

        when(ctx.getTools()).thenReturn(List.of(tool1, tool2));
        when(ctx.getSkills()).thenReturn(List.of(skill1));
        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("test-session"), eq("测试描述"), eq("100"),
                eq(List.of("1", "2")), eq(List.of("10")), isNull()))
                .thenReturn("999");

        Message resultMessage = Message.builder().content("执行成功").build();
        when(callback.execute(eq(ctx), eq("999"), eq("hello"), isNull())).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("执行成功", result);
        verify(ctx).createChildSession("test-session", "测试描述", "100", List.of("1", "2"), List.of("10"), null);
        verify(callback).execute(ctx, "999", "hello", null);
    }

    @Test
    void execute_thinking为true_传递给sendUserMessage() throws Exception {
        String arguments = """
                {
                    "sessionName": "thinking-session",
                    "userMessage": "hello",
                    "thinking": true
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("thinking-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("888");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("888"), eq("hello"), eq(true))).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("ok", result);
        verify(ctx).createChildSession("thinking-session", null, "100", null, null, null);
        verify(callback).execute(ctx, "888", "hello", true);
    }

    @Test
    void execute_thinking为false_传递给sendUserMessage() throws Exception {
        String arguments = """
                {
                    "sessionName": "no-thinking-session",
                    "userMessage": "hello",
                    "thinking": false
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("no-thinking-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("777");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("777"), eq("hello"), eq(false))).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("ok", result);
        verify(ctx).createChildSession("no-thinking-session", null, "100", null, null, null);
        verify(callback).execute(ctx, "777", "hello", false);
    }

    @Test
    void getParameterSchema_包含thinking字段() {
        String schema = tool.getParameterSchema();
        assertTrue(schema.contains("\"thinking\""));
        assertTrue(schema.contains("\"boolean\""));
    }

    @Test
    void execute_toolNames为null时toolIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("test-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("888");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("888"), eq("hello"), isNull())).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("ok", result);
        verify(ctx).createChildSession("test-session", null, "100", null, null, null);
        verify(callback).execute(ctx, "888", "hello", null);
    }

    @Test
    void execute_toolNames为空数组时toolIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello",
                    "toolNames": []
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("test-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("777");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("777"), eq("hello"), isNull())).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, "100", null, null, null);
        verify(callback).execute(ctx, "777", "hello", null);
    }

    @Test
    void execute_skillNames为null时skillIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(anyString(), isNull(), anyString(),
                isNull(), isNull(), isNull()))
                .thenReturn("666");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(any(), anyString(), anyString(), any())).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, "100", null, null, null);
    }

    @Test
    void execute_skillNames为空数组时skillIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello",
                    "skillNames": []
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("test-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("555");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("555"), eq("hello"), isNull())).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, "100", null, null, null);
    }

    @Test
    void execute_description为null时传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("test-session"), isNull(), eq("100"),
                isNull(), isNull(), isNull()))
                .thenReturn("444");

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(eq(ctx), eq("444"), eq("hello"), isNull())).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, "100", null, null, null);
        verify(callback).execute(ctx, "444", "hello", null);
    }

    @Test
    void execute_JSON解析异常_返回错误JSON() {
        String invalidJson = "{invalid}";

        String result = tool.execute(ctx, invalidJson);

        assertTrue(result.contains("error"));
    }

    @Test
    void execute_回调抛出异常_返回错误JSON() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(anyString(), isNull(), anyString(),
                isNull(), isNull(), isNull()))
                .thenReturn("333");
        when(callback.execute(any(), anyString(), anyString(), any())).thenThrow(new RuntimeException("回调失败"));

        String result = tool.execute(ctx, arguments);

        assertTrue(result.contains("error"));
        assertTrue(result.contains("回调失败"));
    }

    @Test
    void execute_toolNames匹配时返回正确的toolIds() throws Exception {
        String arguments = """
                {
                    "sessionName": "s1",
                    "userMessage": "hi",
                    "toolNames": ["tool_a", "tool_b", "tool_c"]
                }
                """;

        ToolConfigDTO ta = ToolConfigDTO.builder().id("1").name("tool_a").build();
        ToolConfigDTO tb = ToolConfigDTO.builder().id("2").name("tool_b").build();
        ToolConfigDTO tc = ToolConfigDTO.builder().id("3").name("tool_c").build();
        ToolConfigDTO td = ToolConfigDTO.builder().id("4").name("tool_d").build();

        when(ctx.getTools()).thenReturn(List.of(ta, tb, tc, td));
        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("s1"), isNull(), eq("100"),
                eq(List.of("1", "2", "3")), isNull(), isNull()))
                .thenReturn("222");

        Message msg = Message.builder().content("done").build();
        when(callback.execute(eq(ctx), eq("222"), eq("hi"), isNull())).thenReturn(msg);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("s1", null, "100", List.of("1", "2", "3"), null, null);
        verify(callback).execute(ctx, "222", "hi", null);
    }

    @Test
    void execute_部分toolNames匹配时过滤掉不存在的() throws Exception {
        String arguments = """
                {
                    "sessionName": "s1",
                    "userMessage": "hi",
                    "toolNames": ["tool_a", "non_existent"]
                }
                """;

        ToolConfigDTO ta = ToolConfigDTO.builder().id("1").name("tool_a").build();

        when(ctx.getTools()).thenReturn(List.of(ta));
        when(ctx.getModelId()).thenReturn("100");
        when(ctx.createChildSession(eq("s1"), isNull(), eq("100"),
                eq(List.of("1")), isNull(), isNull()))
                .thenReturn("111");

        Message msg = Message.builder().content("done").build();
        when(callback.execute(eq(ctx), eq("111"), eq("hi"), isNull())).thenReturn(msg);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("s1", null, "100", List.of("1"), null, null);
        verify(callback).execute(ctx, "111", "hi", null);
    }
}
