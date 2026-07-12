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

        ToolConfigDTO tool1 = ToolConfigDTO.builder().id(1L).name("tool1").build();
        ToolConfigDTO tool2 = ToolConfigDTO.builder().id(2L).name("tool2").build();
        SkillConfigDTO skill1 = SkillConfigDTO.builder().id(10L).name("skill1").build();

        when(ctx.getTools()).thenReturn(List.of(tool1, tool2));
        when(ctx.getSkills()).thenReturn(List.of(skill1));
        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("test-session"), eq("测试描述"), eq(100L),
                eq(List.of(1L, 2L)), eq(List.of(10L)), isNull()))
                .thenReturn(999L);

        Message resultMessage = Message.builder().content("执行成功").build();
        when(callback.execute(999L, "hello")).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("执行成功", result);
        verify(ctx).createChildSession("test-session", "测试描述", 100L, List.of(1L, 2L), List.of(10L), null);
        verify(callback).execute(999L, "hello");
    }

    @Test
    void execute_toolNames为null时toolIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("test-session"), isNull(), eq(100L),
                isNull(), isNull(), isNull()))
                .thenReturn(888L);

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(888L, "hello")).thenReturn(resultMessage);

        String result = tool.execute(ctx, arguments);

        assertEquals("ok", result);
        verify(ctx).createChildSession("test-session", null, 100L, null, null, null);
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

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("test-session"), isNull(), eq(100L),
                isNull(), isNull(), isNull()))
                .thenReturn(777L);

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(777L, "hello")).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, 100L, null, null, null);
    }

    @Test
    void execute_skillNames为null时skillIds传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(anyString(), isNull(), anyLong(),
                isNull(), isNull(), isNull()))
                .thenReturn(666L);

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(anyLong(), anyString())).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, 100L, null, null, null);
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

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("test-session"), isNull(), eq(100L),
                isNull(), isNull(), isNull()))
                .thenReturn(555L);

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(555L, "hello")).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, 100L, null, null, null);
    }

    @Test
    void execute_description为null时传null() throws Exception {
        String arguments = """
                {
                    "sessionName": "test-session",
                    "userMessage": "hello"
                }
                """;

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("test-session"), isNull(), eq(100L),
                isNull(), isNull(), isNull()))
                .thenReturn(444L);

        Message resultMessage = Message.builder().content("ok").build();
        when(callback.execute(444L, "hello")).thenReturn(resultMessage);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("test-session", null, 100L, null, null, null);
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

        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(anyString(), isNull(), anyLong(),
                isNull(), isNull(), isNull()))
                .thenReturn(333L);
        when(callback.execute(anyLong(), anyString())).thenThrow(new RuntimeException("回调失败"));

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

        ToolConfigDTO ta = ToolConfigDTO.builder().id(1L).name("tool_a").build();
        ToolConfigDTO tb = ToolConfigDTO.builder().id(2L).name("tool_b").build();
        ToolConfigDTO tc = ToolConfigDTO.builder().id(3L).name("tool_c").build();
        ToolConfigDTO td = ToolConfigDTO.builder().id(4L).name("tool_d").build();

        when(ctx.getTools()).thenReturn(List.of(ta, tb, tc, td));
        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("s1"), isNull(), eq(100L),
                eq(List.of(1L, 2L, 3L)), isNull(), isNull()))
                .thenReturn(222L);

        Message msg = Message.builder().content("done").build();
        when(callback.execute(222L, "hi")).thenReturn(msg);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("s1", null, 100L, List.of(1L, 2L, 3L), null, null);
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

        ToolConfigDTO ta = ToolConfigDTO.builder().id(1L).name("tool_a").build();

        when(ctx.getTools()).thenReturn(List.of(ta));
        when(ctx.getModelId()).thenReturn(100L);
        when(ctx.createChildSession(eq("s1"), isNull(), eq(100L),
                eq(List.of(1L)), isNull(), isNull()))
                .thenReturn(111L);

        Message msg = Message.builder().content("done").build();
        when(callback.execute(111L, "hi")).thenReturn(msg);

        tool.execute(ctx, arguments);

        verify(ctx).createChildSession("s1", null, 100L, List.of(1L), null, null);
    }
}
