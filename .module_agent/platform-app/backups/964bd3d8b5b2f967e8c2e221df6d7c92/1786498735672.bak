package com.ghost616.platform.service.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.model.SessionMemoryDocument;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.search.SessionMemoryESClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionMemoryServiceTest {

    @Mock
    private AgentConfigMapper agentConfigMapper;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ModelInvoker llmInvoker;
    @Mock
    private ModelInvoker embedInvoker;
    @Mock
    private SessionMemoryESClient sessionMemoryESClient;

    private SessionMemoryService service;

    @BeforeEach
    void setUp() {
        service = new SessionMemoryService(agentConfigMapper, sessionMapper, messageMapper,
                modelConfigMapper, modelInvokerManager, sessionMemoryESClient);
    }

    private AgentConfig memoryAgent(Long id) {
        AgentConfig agent = new AgentConfig();
        agent.setId(id);
        agent.setMemoryEnabled(true);
        agent.setMemoryGroupCount(2);
        agent.setVectorModelId(20L);
        agent.setModelId(10L);
        return agent;
    }

    private ModelConfig llmModel(Long id) {
        ModelConfig m = new ModelConfig();
        m.setId(id);
        m.setModelName("llm-model");
        return m;
    }

    private ModelConfig vectorModel(Long id) {
        ModelConfig m = new ModelConfig();
        m.setId(id);
        m.setModelName("embed-model");
        return m;
    }

    private Session session(Long id, Long agentId, Integer memoryPoint) {
        Session s = new Session();
        s.setId(id);
        s.setAgentId(agentId);
        s.setModelId(10L);
        s.setMemoryPointSequenceNum(memoryPoint);
        return s;
    }

    private Message message(Long id, Long sessionId, String role, String content, int seq, boolean rollback) {
        Message m = new Message();
        m.setId(id);
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setSequenceNum(seq);
        m.setRollback(rollback);
        return m;
    }

    private void stubInvokers() {
        when(modelConfigMapper.selectById(10L)).thenReturn(llmModel(10L));
        when(modelConfigMapper.selectById(20L)).thenReturn(vectorModel(20L));
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenAnswer(invocation -> {
            ModelConfigData data = invocation.getArgument(0);
            return "embed-model".equals(data.modelName()) ? embedInvoker : llmInvoker;
        });
        when(embedInvoker.embed(any(EmbeddingRequest.class))).thenReturn(
                EmbeddingResponse.builder().embeddings(List.of(
                        EmbeddingResponse.EmbeddingItem.builder().index(0).embedding(List.of(0.1f, 0.2f)).build()))
                        .build());
    }

    @Test
    @DisplayName("无 memoryEnabled=true 的智能体时直接返回")
    void aggregate_noMemoryAgents_return() {
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.aggregateSessionMemories();

        verify(sessionMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("有新消息时按组内容-主题归类-大组汇总生成文档并写入ES、更新记忆点")
    void aggregate_newMessages_writesAndUpdates() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(3L);
        when(messageMapper.findNthUserSequenceNum(100L, 1)).thenReturn(4);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 技术").build())
                .thenReturn(ChatResponse.builder().content("用户完成了数据库配置，结论为使用连接池。").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(1, docs.size());
        SessionMemoryDocument doc = docs.get(0);
        assertEquals("100", doc.getSessionId());
        assertEquals(2, doc.getAggregationStartSeq());
        assertEquals(3, doc.getAggregationEndSeq());
        assertEquals("用户完成了数据库配置，结论为使用连接池。", doc.getAggregationText());
        assertEquals(2, doc.getVector().size());
        verify(llmInvoker, times(2)).invoke(any(ChatRequest.class));

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertEquals(4, sessionCaptor.getValue().getMemoryPointSequenceNum());
    }

    @Test
    @DisplayName("记忆点不变时跳过")
    void aggregate_noNewMessages_skip() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 4);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(2L);

        service.aggregateSessionMemories();

        verify(sessionMemoryESClient, never()).batchSave(any());
        verify(sessionMapper, never()).updateById(any(Session.class));
    }

    @Test
    @DisplayName("未配置向量模型时跳过")
    void aggregate_noVectorModel_skip() {
        AgentConfig agent = memoryAgent(5L);
        agent.setVectorModelId(null);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 3);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        service.aggregateSessionMemories();

        verify(sessionMemoryESClient, never()).batchSave(any());
        verify(sessionMapper, never()).updateById(any(Session.class));
    }

    @Test
    @DisplayName("会话处理失败重试5次后记录日志")
    void aggregate_failure_retries5Times() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 3);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenThrow(new RuntimeException("boom"));

        service.aggregateSessionMemories();

        verify(messageMapper, times(5)).countUserMessages(100L);
        verify(sessionMemoryESClient, never()).batchSave(any());
    }

    @Test
    @DisplayName("主题归类 LLM 返回 null 时回退为每组合并前各自成组，直接使用原始内容")
    void aggregate_summaryFail_skipGroup() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class))).thenReturn(ChatResponse.builder().content(null).build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(2, docs.size());
        assertEquals("【user】: q1\n【assistant】: a1", docs.get(0).getAggregationText());
        assertEquals("【user】: q2\n【assistant】: a2", docs.get(1).getAggregationText());
        verify(llmInvoker, times(1)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("多组消息归类为同一主题时合并为一个文档，startSeq/endSeq 覆盖完整区间")
    void aggregate_multipleGroupsSameTopic_mergeToOneDocument() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 数据库\n2. 数据库").build())
                .thenReturn(ChatResponse.builder().content("汇总：围绕数据库索引与查询性能展开了深入讨论。").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(1, docs.size());
        SessionMemoryDocument doc = docs.get(0);
        assertEquals(2, doc.getAggregationStartSeq());
        assertEquals(5, doc.getAggregationEndSeq());
        assertEquals("汇总：围绕数据库索引与查询性能展开了深入讨论。", doc.getAggregationText());
        verify(llmInvoker, times(2)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("多组消息归类为不同主题时分别生成文档")
    void aggregate_multipleGroupsDifferentTopics_multipleDocuments() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 行政\n2. 项目").build())
                .thenReturn(ChatResponse.builder().content("汇总1：团建安排已确认。").build())
                .thenReturn(ChatResponse.builder().content("汇总2：项目上线时间确定为周五。").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(2, docs.size());
        assertEquals(2, docs.get(0).getAggregationStartSeq());
        assertEquals(3, docs.get(0).getAggregationEndSeq());
        assertEquals("汇总1：团建安排已确认。", docs.get(0).getAggregationText());
        assertEquals(4, docs.get(1).getAggregationStartSeq());
        assertEquals(5, docs.get(1).getAggregationEndSeq());
        assertEquals("汇总2：项目上线时间确定为周五。", docs.get(1).getAggregationText());
        verify(llmInvoker, times(3)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("主题归类结果无法解析时每组合并前各自成组，跳过汇总直接使用原始内容")
    void aggregate_unparseableTopics_eachGroupSeparate() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("无法识别的归类输出").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(2, docs.size());
        assertEquals(2, docs.get(0).getAggregationStartSeq());
        assertEquals(3, docs.get(0).getAggregationEndSeq());
        assertEquals("【user】: q1\n【assistant】: a1", docs.get(0).getAggregationText());
        assertEquals(4, docs.get(1).getAggregationStartSeq());
        assertEquals(5, docs.get(1).getAggregationEndSeq());
        assertEquals("【user】: q2\n【assistant】: a2", docs.get(1).getAggregationText());
        verify(llmInvoker, times(1)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("主题归类结果数量不匹配时回退为每组合并前各自成组，跳过汇总直接使用原始内容")
    void aggregate_mismatchedTopicCount_eachGroupSeparate() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 技术").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(2, docs.size());
        assertEquals(2, docs.get(0).getAggregationStartSeq());
        assertEquals(3, docs.get(0).getAggregationEndSeq());
        assertEquals("【user】: q1\n【assistant】: a1", docs.get(0).getAggregationText());
        assertEquals(4, docs.get(1).getAggregationStartSeq());
        assertEquals(5, docs.get(1).getAggregationEndSeq());
        assertEquals("【user】: q2\n【assistant】: a2", docs.get(1).getAggregationText());
        verify(llmInvoker, times(1)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("LLM 乱序输出主题时按序号映射而非按顺位匹配，同主题仍合并")
    void aggregate_outOfOrderTopics_mapByIndex() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("2. 数据库\n1. 数据库").build())
                .thenReturn(ChatResponse.builder().content("汇总：两组均为数据库优化话题。").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(1, docs.size());
        assertEquals(2, docs.get(0).getAggregationStartSeq());
        assertEquals(5, docs.get(0).getAggregationEndSeq());
        assertEquals("汇总：两组均为数据库优化话题。", docs.get(0).getAggregationText());
        verify(llmInvoker, times(2)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("主题归类输出包含空白主题时按空白过滤后数量不足则回退为每组合并前各自成组")
    void aggregate_blankTopicFiltered_eachGroupSeparate() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(4L);
        when(messageMapper.findNthUserSequenceNum(100L, 2)).thenReturn(6);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 数据库\n2. ").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(2, docs.size());
        assertEquals("【user】: q1\n【assistant】: a1", docs.get(0).getAggregationText());
        assertEquals("【user】: q2\n【assistant】: a2", docs.get(1).getAggregationText());
        verify(llmInvoker, times(1)).invoke(any(ChatRequest.class));
    }

    @Test
    @DisplayName("主题序列 A→B→A 时不相邻的同主题不合并，连续合并切分为三段")
    void aggregate_sameTopicNonAdjacent_splitByContiguity() {
        AgentConfig agent = memoryAgent(5L);
        when(agentConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(agent));
        Session s = session(100L, 5L, 1);
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s));
        when(agentConfigMapper.selectBatchIds(any())).thenReturn(List.of(agent));

        when(messageMapper.countUserMessages(100L)).thenReturn(5L);
        when(messageMapper.findNthUserSequenceNum(100L, 3)).thenReturn(8);
        List<Message> newMessages = List.of(
                message(2L, 100L, "user", "q1", 2, false),
                message(3L, 100L, "assistant", "a1", 3, false),
                message(4L, 100L, "user", "q2", 4, false),
                message(5L, 100L, "assistant", "a2", 5, false),
                message(6L, 100L, "user", "q3", 6, false),
                message(7L, 100L, "assistant", "a3", 7, false));
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(newMessages);

        stubInvokers();
        when(llmInvoker.invoke(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().content("1. 技术\n2. 项目\n3. 技术").build())
                .thenReturn(ChatResponse.builder().content("汇总A1").build())
                .thenReturn(ChatResponse.builder().content("汇总B").build())
                .thenReturn(ChatResponse.builder().content("汇总A2").build());

        service.aggregateSessionMemories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SessionMemoryDocument>> docCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionMemoryESClient).batchSave(docCaptor.capture());
        List<SessionMemoryDocument> docs = docCaptor.getValue();
        assertEquals(3, docs.size());
        assertEquals(2, docs.get(0).getAggregationStartSeq());
        assertEquals(3, docs.get(0).getAggregationEndSeq());
        assertEquals("汇总A1", docs.get(0).getAggregationText());
        assertEquals(4, docs.get(1).getAggregationStartSeq());
        assertEquals(5, docs.get(1).getAggregationEndSeq());
        assertEquals("汇总B", docs.get(1).getAggregationText());
        assertEquals(6, docs.get(2).getAggregationStartSeq());
        assertEquals(7, docs.get(2).getAggregationEndSeq());
        assertEquals("汇总A2", docs.get(2).getAggregationText());
        verify(llmInvoker, times(4)).invoke(any(ChatRequest.class));
    }
}
