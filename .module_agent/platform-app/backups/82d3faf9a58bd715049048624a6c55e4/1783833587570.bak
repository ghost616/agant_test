package com.ghost616.platform.service.agent.invoker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCallDelta;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;


@ExtendWith(MockitoExtension.class)
class SpawnSubAgentSystemToolTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ToolManager toolManager;
    @Mock
    private ModelInvoker modelInvoker;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SpawnSubAgentSystemTool tool;

    private AgentExecutionContext context;
    private ModelConfig modelConfig;
    private AgentExecutionContext.AgentContextMutator mutator;

    @BeforeEach
    void setUp() {
        mutator = new AgentExecutionContext.AgentContextMutator();
        context = createContextWithTools(new ArrayList<>());
        modelConfig = new ModelConfig();
        modelConfig.setId(1L);
    }

    // === 1. ToolManager 注入 ===
    @Test
    void toolManager注入不为null() {
        assertNotNull(tool);
    }

    // === 2. _sys_ 前缀工具过滤 ===
    @Test
    void sys前缀工具被过滤不加入toolDefs() {
        context = createContextWithTools(List.of(
                ToolConfigDTO.builder().name("get_weather").build(),
                ToolConfigDTO.builder().name("_sys_spawn").build()
        ));

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(ChatChunk.builder().delta("ok").build()));

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"do\"}");

        assertTrue(result.contains("\"status\":\"ok\""));
        verify(modelInvoker, times(1)).toToolDefinition(any());
    }

    @Test
    void 全部为sys工具时toolDefs为空_tools为null() {
        context = createContextWithTools(List.of(
                ToolConfigDTO.builder().name("_sys_spawn").build(),
                ToolConfigDTO.builder().name("_sys_stop").build()
        ));

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(ChatChunk.builder().delta("ok").build()));

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"do\"}");

        assertTrue(result.contains("\"status\":\"ok\""));
        verify(modelInvoker, never()).toToolDefinition(any());
    }

    // === 3. 流式 chunk tool_calls 累积解析 ===
    @Test
    void 单个tool_call跨多个chunk时正确累积id_name_arguments() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk c1 = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("get_weather").build()
                )).build();
        ChatChunk c2 = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).arguments("{\"loc").build()
                )).build();
        ChatChunk c3 = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).arguments("ation\":\"BJ\"}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(c1, c2, c3))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker mockInvoker = mock(ToolInvoker.class);
        when(mockInvoker.execute(any(), any())).thenReturn("ok");
        when(toolManager.getInvoker(any(), any())).thenReturn(mockInvoker);

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"do\"}");

        verify(mockInvoker).execute(any(), eq("{\"location\":\"BJ\"}"));
    }

    @Test
    void index作为key_id为空时最终id回退为index字符串() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).name("no_id_tool").arguments("{}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker mockInvoker = mock(ToolInvoker.class);
        when(mockInvoker.execute(any(), any())).thenReturn("ok");
        when(toolManager.getInvoker(any(), any())).thenReturn(mockInvoker);

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"do\"}");

        assertTrue(result.contains("\"status\":\"ok\""));
    }

    @Test
    void 多个tool_call并行时各自独立累积() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("tool_a").arguments("{\"a\":1}").build(),
                        ToolCallDelta.builder().index(1).id("call_2").name("tool_b").arguments("{\"b\":2}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker invokerA = mock(ToolInvoker.class);
        ToolInvoker invokerB = mock(ToolInvoker.class);
        when(invokerA.execute(any(), any())).thenReturn("ok");
        when(invokerB.execute(any(), any())).thenReturn("ok");
        when(toolManager.getInvoker(eq(1L), eq("tool_a"))).thenReturn(invokerA);
        when(toolManager.getInvoker(eq(1L), eq("tool_b"))).thenReturn(invokerB);

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"do\"}");

        verify(invokerA).execute(any(), eq("{\"a\":1}"));
        verify(invokerB).execute(any(), eq("{\"b\":2}"));
    }

    // === 4. 无 tool_calls 直接返回 ===
    @Test
    void 无tool_calls时直接返回content() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(ChatChunk.builder().delta("Hello world").build()));

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"say hello\"}");

        assertTrue(result.contains("\"content\":\"Hello world\""));
        assertTrue(result.contains("\"status\":\"ok\""));
    }

    // === 5. 有 tool_calls 多轮循环 ===
    @Test
    void tool_calls触发后执行工具并回传结果进入第二轮() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk round1 = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("calculator").arguments("{\"x\":1}").build()
                )).build();
        ChatChunk round2 = ChatChunk.builder().delta("Result is 2").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(round1))
                .thenReturn(Flux.just(round2));

        ToolInvoker calcInvoker = mock(ToolInvoker.class);
        when(calcInvoker.execute(any(), any())).thenReturn("2");
        when(toolManager.getInvoker(any(), any())).thenReturn(calcInvoker);

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"calculate\"}");

        assertTrue(result.contains("\"content\":\"Result is 2\""));
        assertTrue(result.contains("\"status\":\"ok\""));
        verify(modelInvoker, times(2)).invokeStream(any());
    }

    // === 6. 达到最大迭代次数 ===
    @Test
    void 达到10次最大迭代次数返回max_iterations() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk toolCallChunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("loop_tool").arguments("{}").build()
                )).build();

        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(toolCallChunk));

        ToolInvoker loopInvoker = mock(ToolInvoker.class);
        when(loopInvoker.execute(any(), any())).thenReturn("again");
        when(toolManager.getInvoker(any(), any())).thenReturn(loopInvoker);

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"loop\"}");

        assertTrue(result.contains("\"status\":\"max_iterations\""));
        verify(modelInvoker, times(10)).invokeStream(any());
    }

    // === 7. 工具未找到 ===
    @Test
    void 工具未找到时返回error并继续循环() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("nonexistent").arguments("{}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("recovered").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));
        when(toolManager.getInvoker(any(), any())).thenReturn(null);

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"find tool\"}");

        assertTrue(result.contains("\"status\":\"ok\""));
        assertTrue(result.contains("recovered"));
    }

    // === 8. 工具执行异常 ===
    @Test
    void 工具执行异常时捕获异常返回error并继续循环() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("failing_tool").arguments("{}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("recovered").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker failingInvoker = mock(ToolInvoker.class);
        when(failingInvoker.execute(any(), any())).thenThrow(new RuntimeException("Something went wrong"));
        when(toolManager.getInvoker(any(), any())).thenReturn(failingInvoker);

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"fail\"}");

        assertTrue(result.contains("\"status\":\"ok\""));
        assertTrue(result.contains("recovered"));
    }

    // === 9. reasoning 累积与透传 ===
    @Test
    void reasoning跨多chunk正确累积到assistant消息() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk r1 = ChatChunk.builder().delta("Hello").reasoning("Step 1: think").toolCalls(List.of(
                ToolCallDelta.builder().index(0).id("call_1").name("tool_x").arguments("{}").build()
        )).build();
        ChatChunk r2 = ChatChunk.builder().delta(" world").reasoning("Step 2: decide").build();
        ChatChunk finalChunk = ChatChunk.builder().delta("Done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(r1, r2))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker mockInvoker = mock(ToolInvoker.class);
        when(mockInvoker.execute(any(), any())).thenReturn("ok");
        when(toolManager.getInvoker(any(), any())).thenReturn(mockInvoker);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"think\"}");

        verify(modelInvoker, times(2)).invokeStream(captor.capture());
        ChatRequest secondReq = captor.getAllValues().get(1);
        Message assistantMsg = secondReq.getMessages().stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .findFirst().orElse(null);
        assertNotNull(assistantMsg);
        assertEquals("Hello world", assistantMsg.getContent());
        assertEquals("Step 1: thinkStep 2: decide", assistantMsg.getReasoning());
    }

    @Test
    void reasoning为null时不影响content和toolCalls构建() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder().delta("Just content").build();
        ChatChunk finalChunk = ChatChunk.builder().delta("done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));

        String result = tool.execute(context, "{\"agentName\":\"test\",\"task\":\"say\"}");
        assertTrue(result.contains("\"content\":\"Just content\""));
        assertTrue(result.contains("\"status\":\"ok\""));
    }

    @Test
    void reasoning与toolCalls同时存在时均正确构建() {
        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .delta("Answer")
                .reasoning("Deep thinking...")
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("search").arguments("{\"q\":\"test\"}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("Done").build();

        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));

        ToolInvoker mockInvoker = mock(ToolInvoker.class);
        when(mockInvoker.execute(any(), any())).thenReturn("result");
        when(toolManager.getInvoker(any(), any())).thenReturn(mockInvoker);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"search\"}");

        verify(modelInvoker, times(2)).invokeStream(captor.capture());
        ChatRequest secondReq = captor.getAllValues().get(1);
        Message assistantMsg = secondReq.getMessages().stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .findFirst().orElse(null);
        assertNotNull(assistantMsg);
        assertEquals("Answer", assistantMsg.getContent());
        assertEquals("Deep thinking...", assistantMsg.getReasoning());
        assertNotNull(assistantMsg.getToolCalls());
        assertFalse(assistantMsg.getToolCalls().isEmpty());
    }

    // === 10. 诊断日志输出 ===
    static class MemoryAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }
    }

    @Test
    void 迭代开始输出日志含迭代次数和消息列表长度() {
        MemoryAppender appender = new MemoryAppender();
        Logger logger = (Logger) LoggerFactory.getLogger(SpawnSubAgentSystemTool.class);
        logger.addAppender(appender);
        appender.start();

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(ChatChunk.builder().delta("ok").build()));

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"log test\"}");

        assertTrue(appender.events.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO
                        && e.getFormattedMessage().contains("子智能体迭代")
                        && e.getFormattedMessage().contains("/10")));

        logger.detachAppender(appender);
    }

    @Test
    void LLM调用前后分别输出日志() {
        MemoryAppender appender = new MemoryAppender();
        Logger logger = (Logger) LoggerFactory.getLogger(SpawnSubAgentSystemTool.class);
        logger.addAppender(appender);
        appender.start();

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(ChatChunk.builder().delta("ok").build()));

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"log llm\"}");

        long beforeCount = appender.events.stream()
                .filter(e -> e.getFormattedMessage().contains("LLM 调用开始")).count();
        long afterCount = appender.events.stream()
                .filter(e -> e.getFormattedMessage().contains("LLM 调用完成")).count();
        assertTrue(beforeCount >= 1);
        assertTrue(afterCount >= 1);

        logger.detachAppender(appender);
    }

    @Test
    void 工具执行前后输出日志含工具名和参数长度() {
        MemoryAppender appender = new MemoryAppender();
        Logger logger = (Logger) LoggerFactory.getLogger(SpawnSubAgentSystemTool.class);
        logger.addAppender(appender);
        appender.start();

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("my_tool").arguments("{\"a\":1}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("done").build();
        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));
        ToolInvoker mockInvoker = mock(ToolInvoker.class);
        when(mockInvoker.execute(any(), any())).thenReturn("tool_result_ok");
        when(toolManager.getInvoker(any(), any())).thenReturn(mockInvoker);

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"tool log\"}");

        assertTrue(appender.events.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("执行工具")
                        && e.getFormattedMessage().contains("my_tool")
                        && e.getFormattedMessage().contains("参数长度")));
        assertTrue(appender.events.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("my_tool 执行完成")
                        && e.getFormattedMessage().contains("结果长度")));

        logger.detachAppender(appender);
    }

    @Test
    void 异常日志包含迭代次数和工具名上下文() {
        MemoryAppender appender = new MemoryAppender();
        Logger logger = (Logger) LoggerFactory.getLogger(SpawnSubAgentSystemTool.class);
        logger.addAppender(appender);
        appender.start();

        when(modelConfigMapper.selectById(any())).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);
        ChatChunk chunk = ChatChunk.builder()
                .toolCalls(List.of(
                        ToolCallDelta.builder().index(0).id("call_1").name("bad_tool").arguments("{}").build()
                )).build();
        ChatChunk finalChunk = ChatChunk.builder().delta("recovered").build();
        when(modelInvoker.invokeStream(any()))
                .thenReturn(Flux.just(chunk))
                .thenReturn(Flux.just(finalChunk));
        ToolInvoker failingInvoker = mock(ToolInvoker.class);
        when(failingInvoker.execute(any(), any())).thenThrow(new RuntimeException("failure detail"));
        when(toolManager.getInvoker(any(), any())).thenReturn(failingInvoker);

        tool.execute(context, "{\"agentName\":\"test\",\"task\":\"error log\"}");

        assertTrue(appender.events.stream()
                .anyMatch(e -> e.getLevel() == Level.ERROR
                        && e.getFormattedMessage().contains("迭代 1")
                        && e.getFormattedMessage().contains("bad_tool")
                        && e.getFormattedMessage().contains("failure detail")));

        logger.detachAppender(appender);
    }

    private AgentExecutionContext createContextWithTools(List<ToolConfigDTO> tools) {
        return new AgentExecutionContext(
                1L, 1L, "system prompt", 1L, 10,
                new ArrayList<>(), tools, new ArrayList<>(),
                mutator, new HashMap<>(), new HashMap<>(), null, null);
    }
}
