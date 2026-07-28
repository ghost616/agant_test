package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.entity.Evaluation;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.EvaluationMapper;
import com.ghost616.platform.repository.EvaluationResultMapper;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionSkillMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluationExecutionServiceTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private EvaluationResultMapper evaluationResultMapper;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private SessionToolMapper sessionToolMapper;
    @Mock
    private SessionSkillMapper sessionSkillMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private MessageToolCallMapper messageToolCallMapper;
    @Mock
    private MessageDataProvider messageDataProvider;

    private EvaluationExecutionService service;

    private static final Long EVALUATION_ID = 1L;
    private static final Long BENCHMARK_SESSION_ID = 100L;
    private static final Long EXECUTION_SESSION_ID = 200L;

    @BeforeEach
    void setUp() {
        service = spy(new EvaluationExecutionService(
                evaluationMapper, evaluationResultMapper, sessionMapper,
                sessionToolMapper, sessionSkillMapper, messageMapper,
                messageToolCallMapper, messageDataProvider, null, null, null
        ));
        doNothing().when(service).asyncExecute(anyLong(), anyLong(), anyList());
    }

    private Evaluation createEvaluation(Long benchmarkSessionId) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(EVALUATION_ID);
        evaluation.setBenchmarkSessionId(benchmarkSessionId);
        return evaluation;
    }

    private MessageDataProvider.MessageDTO createUserMessage(String content) {
        return new MessageDataProvider.MessageDTO(
                "1", String.valueOf(BENCHMARK_SESSION_ID), "user", content,
                null, null, null, null, null, null, null, null
        );
    }

    private MessageDataProvider.MessageDTO createAssistantMessage(String content) {
        return new MessageDataProvider.MessageDTO(
                "2", String.valueOf(BENCHMARK_SESSION_ID), "assistant", content,
                null, null, null, null, null, null, null, null
        );
    }

    @Nested
    class ExecuteTests {

        @Test
        void evaluationNotFound_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class, () -> service.execute(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void benchmarkSessionIdNull_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(createEvaluation(null));
            BusinessException ex = assertThrows(BusinessException.class, () -> service.execute(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void noUserMessages_shouldThrow() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createAssistantMessage("hello")));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.execute(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_BENCHMARK_NO_USER_MESSAGE, ex.getErrorCode());
        }

        @Test
        void normalExecution_shouldReturnStatusDTO() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createUserMessage("test message")));
            when(sessionMapper.selectById(BENCHMARK_SESSION_ID)).thenReturn(createBenchmarkSession());
            doAnswer(inv -> {
                Session s = inv.getArgument(0);
                s.setId(EXECUTION_SESSION_ID);
                return null;
            }).when(sessionMapper).insert(any(Session.class));

            EvaluationExecutionStatusDTO result = service.execute(EVALUATION_ID);

            assertNotNull(result);
            assertEquals(EVALUATION_ID, result.getEvaluationId());
            assertEquals(EXECUTION_SESSION_ID, result.getExecutionSessionId());
            assertEquals("PENDING", result.getStatus());
            assertEquals(0, result.getCurrentStep());
            assertEquals(1, result.getTotalSteps());
        }
    }

    @Nested
    class GetStatusTests {

        @Test
        void nonExistentEvaluationId_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.getStatus(999L));
            assertEquals(ErrorCode.EVALUATION_EXECUTION_STATUS_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void existingStatus_shouldReturnStatusDTO() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createUserMessage("test")));
            when(sessionMapper.selectById(BENCHMARK_SESSION_ID)).thenReturn(createBenchmarkSession());
            doAnswer(inv -> {
                Session s = inv.getArgument(0);
                s.setId(EXECUTION_SESSION_ID);
                return null;
            }).when(sessionMapper).insert(any(Session.class));

            service.execute(EVALUATION_ID);

            EvaluationExecutionStatusDTO result = service.getStatus(EVALUATION_ID);
            assertNotNull(result);
            assertEquals(EVALUATION_ID, result.getEvaluationId());
            assertEquals("PENDING", result.getStatus());
        }
    }

    @Nested
    class CreateExecutionSessionTests {

        @Test
        void evaluationNotFound_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class, () -> service.createExecutionSession(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void benchmarkSessionIdNull_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(createEvaluation(null));
            BusinessException ex = assertThrows(BusinessException.class, () -> service.createExecutionSession(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void noUserMessages_shouldThrow() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createAssistantMessage("hello")));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.createExecutionSession(EVALUATION_ID));
            assertEquals(ErrorCode.EVALUATION_BENCHMARK_NO_USER_MESSAGE, ex.getErrorCode());
        }

        @Test
        void normalCreation_shouldReturnSessionId() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createUserMessage("test")));
            when(sessionMapper.selectById(BENCHMARK_SESSION_ID)).thenReturn(createBenchmarkSession());
            doAnswer(inv -> {
                Session s = inv.getArgument(0);
                s.setId(EXECUTION_SESSION_ID);
                return null;
            }).when(sessionMapper).insert(any(Session.class));

            Long result = service.createExecutionSession(EVALUATION_ID);
            assertEquals(EXECUTION_SESSION_ID, result);
        }
    }

    @Nested
    class GenerateResultTests {

        @Test
        void shouldDelegateToGenerateService() {
            EvaluationResultGenerateService generateService = mock(EvaluationResultGenerateService.class);
            EvaluationExecutionService localService = new EvaluationExecutionService(
                    evaluationMapper, evaluationResultMapper, sessionMapper,
                    sessionToolMapper, sessionSkillMapper, messageMapper,
                    messageToolCallMapper, messageDataProvider, null, null, generateService
            );
            localService.generateResult(EVALUATION_ID, EXECUTION_SESSION_ID);
            verify(generateService).generate(EVALUATION_ID, EXECUTION_SESSION_ID);
        }
    }

    private Session createBenchmarkSession() {
        Session session = new Session();
        session.setId(BENCHMARK_SESSION_ID);
        session.setAgentId(50L);
        session.setModelId(1L);
        session.setTitle("benchmark");
        session.setSystemPrompt("You are a test assistant");
        return session;
    }
}
