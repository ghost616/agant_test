package com.ghost616.platform.service.agent_log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.agent_log.AgentLogDTO;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.AgentLogMapper;
import com.ghost616.platform.repository.SessionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentLogServiceImplTest {

    @Mock
    private AgentLogMapper agentLogMapper;

    @Mock
    private SessionMapper sessionMapper;

    private AgentLogServiceImpl agentLogService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AgentLogEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Session.class);
        agentLogService = new AgentLogServiceImpl(agentLogMapper, sessionMapper);
    }

    private AgentLogEntity buildEntity(Long id, Long sessionId, String conversationId,
                                       String logType, String logLevel, LocalDateTime createTime) {
        AgentLogEntity entity = new AgentLogEntity();
        entity.setId(id);
        entity.setSessionId(sessionId);
        entity.setConversationId(conversationId);
        entity.setLogType(logType);
        entity.setLogLevel(logLevel);
        entity.setLogData("{}");
        entity.setCreateTime(createTime);
        return entity;
    }

    private void stubSelectPage() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(buildEntity(1L, 10L, "conv-1", "MODEL_CALL", "INFO",
                    LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });
    }

    @Test
    void list_分页参数正确传递并返回PageResult() {
        stubSelectPage();

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 2, 15);

        ArgumentCaptor<Page<AgentLogEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(agentLogMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        Page<AgentLogEntity> capturedPage = pageCaptor.getValue();
        assertEquals(2, capturedPage.getCurrent());
        assertEquals(15, capturedPage.getSize());

        assertEquals(1, result.getList().size());
        assertEquals(1, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(15, result.getSize());
    }

    @Test
    void list_非法的page和size使用默认值() {
        stubSelectPage();

        agentLogService.list(null, null, null, null, null, 0, 0);

        ArgumentCaptor<Page<AgentLogEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(agentLogMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        Page<AgentLogEntity> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(20, capturedPage.getSize());
    }

    @Test
    void list_筛选条件非空时构造对应eq条件() {
        stubSelectPage();

        agentLogService.list(10L, null, "conv-1", "MODEL_CALL", "INFO", 1, 20);

        ArgumentCaptor<LambdaQueryWrapper<AgentLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("session_id"), "应包含 session_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("conversation_id"), "应包含 conversation_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("log_type"), "应包含 log_type 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("log_level"), "应包含 log_level 条件: " + sqlSegment);
    }

    @Test
    void list_筛选条件为空时不添加对应条件() {
        stubSelectPage();

        agentLogService.list(null, null, null, "  ", null, 1, 20);

        ArgumentCaptor<LambdaQueryWrapper<AgentLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertFalse(sqlSegment.contains("session_id"), "不应包含 session_id 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("conversation_id"), "不应包含 conversation_id 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("log_type"), "不应包含 log_type 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("log_level"), "不应包含 log_level 条件: " + sqlSegment);
    }

    @Test
    void list_session存在时填充sessionName() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(
                    buildEntity(1L, 10L, "conv-1", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });
        Session session = new Session();
        session.setId(10L);
        session.setTitle("测试会话");
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(session));

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertEquals(1, result.getList().size());
        assertEquals("测试会话", result.getList().get(0).getSessionName());
        ArgumentCaptor<Collection> idCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(sessionMapper).selectBatchIds(idCaptor.capture());
        assertTrue(idCaptor.getValue().contains(10L));
    }

    @Test
    void list_session标题为null或空时sessionName兜底为id字符串() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(
                    buildEntity(1L, 10L, "conv-1", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0)),
                    buildEntity(2L, 20L, "conv-2", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(2);
            return page;
        });
        Session session1 = new Session();
        session1.setId(10L);
        session1.setTitle(null);
        Session session2 = new Session();
        session2.setId(20L);
        session2.setTitle("");
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(session1, session2));

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertEquals(2, result.getList().size());
        assertEquals("10", result.getList().get(0).getSessionName());
        assertEquals("20", result.getList().get(1).getSessionName());
    }

    @Test
    void list_session标题为空白时仍使用标题() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(
                    buildEntity(1L, 10L, "conv-1", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });
        Session session = new Session();
        session.setId(10L);
        session.setTitle("   ");
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(session));

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertEquals("   ", result.getList().get(0).getSessionName());
    }

    @Test
    void list_DTO映射sessionVariables和conversationVariables() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            AgentLogEntity entity = buildEntity(1L, 10L, "conv-1", "ROUTE", "INFO",
                    LocalDateTime.of(2026, 8, 9, 0, 0));
            entity.setSessionVariables("{\"skill\":\"java\"}");
            entity.setConversationVariables("{\"topic\":\"log\"}");
            page.setRecords(List.of(entity));
            page.setTotal(1);
            return page;
        });
        Session session = new Session();
        session.setId(10L);
        session.setTitle("测试会话");
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(session));

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertEquals("{\"skill\":\"java\"}", result.getList().get(0).getSessionVariables());
        assertEquals("{\"topic\":\"log\"}", result.getList().get(0).getConversationVariables());
    }

    @Test
    void list_session不存在时sessionName为null() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(
                    buildEntity(1L, 999L, "conv-1", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertNull(result.getList().get(0).getSessionName());
    }

    @Test
    void list_无sessionId记录时不查询session() {
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(buildEntity(1L, null, "conv-1", "ROUTE", "INFO",
                    LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });

        PageResult<AgentLogDTO> result = agentLogService.list(null, null, null, null, null, 1, 20);

        assertNull(result.getList().get(0).getSessionName());
        verify(sessionMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    void list_sessionName匹配时按匹配到的sessionId过滤() {
        Session session1 = new Session();
        session1.setId(10L);
        session1.setTitle("测试会话A");
        Session session2 = new Session();
        session2.setId(20L);
        session2.setTitle("测试会话B");
        when(sessionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(session1, session2));
        when(agentLogMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(inv -> {
            Page<AgentLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(
                    buildEntity(1L, 10L, "conv-1", "ROUTE", "INFO",
                            LocalDateTime.of(2026, 8, 9, 0, 0))));
            page.setTotal(1);
            return page;
        });
        when(sessionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(session1));

        PageResult<AgentLogDTO> result = agentLogService.list(null, "测试会话", null, null, null, 1, 20);

        ArgumentCaptor<LambdaQueryWrapper<AgentLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<AgentLogEntity> capturedWrapper = wrapperCaptor.getValue();
        String sqlSegment = capturedWrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("session_id"), "应包含 session_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("IN"), "应为 IN 条件: " + sqlSegment);
        assertTrue(capturedWrapper.getParamNameValuePairs().values().contains(10L),
                "IN 参数应包含匹配的 sessionId 10: " + capturedWrapper.getParamNameValuePairs());
        assertTrue(capturedWrapper.getParamNameValuePairs().values().contains(20L),
                "IN 参数应包含匹配的 sessionId 20: " + capturedWrapper.getParamNameValuePairs());
        assertEquals(1, result.getList().size());
        assertEquals("测试会话A", result.getList().get(0).getSessionName());
    }

    @Test
    void list_sessionName匹配不到任何会话时返回空分页结果() {
        when(sessionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        PageResult<AgentLogDTO> result = agentLogService.list(null, "不存在的会话", null, null, null, 1, 20);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
        verify(agentLogMapper, never()).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void list_sessionName为空时不查询session() {
        stubSelectPage();

        agentLogService.list(null, "  ", null, null, null, 1, 20);

        verify(sessionMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void cleanupExpiredLogs_删除30天前的记录() {
        agentLogService.cleanupExpiredLogs();

        ArgumentCaptor<LambdaQueryWrapper<AgentLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(agentLogMapper).delete(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("create_time"), "应包含 create_time 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("<"), "应为小于条件: " + sqlSegment);
    }
}
