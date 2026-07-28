package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.platform.entity.Evaluation;
import com.ghost616.platform.entity.EvaluationResult;
import com.ghost616.platform.repository.EvaluationMapper;
import com.ghost616.platform.repository.EvaluationResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationResultGenerateServiceTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private EvaluationResultMapper evaluationResultMapper;
    @Mock
    private MessageDataProvider messageDataProvider;
    @Mock
    private ChatDataProvider chatDataProvider;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ModelInvoker modelInvoker;

    private EvaluationResultGenerateService service;

    private static final Long EVALUATION_ID = 1L;
    private static final Long EXECUTION_SESSION_ID = 200L;
    private static final Long BENCHMARK_SESSION_ID = 100L;
    private static final Long MODEL_ID = 10L;

    @Captor
    private ArgumentCaptor<EvaluationResult> resultCaptor;

    @BeforeEach
    void setUp() {
        service = new EvaluationResultGenerateService(
                evaluationMapper, evaluationResultMapper, messageDataProvider,
                chatDataProvider, modelInvokerManager
        );
    }

    private Evaluation createEvaluation(Long benchmarkSessionId) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(EVALUATION_ID);
        evaluation.setBenchmarkSessionId(benchmarkSessionId);
        evaluation.setModelId(MODEL_ID);
        return evaluation;
    }

    private MessageDataProvider.MessageDTO createMessage(String role, String content) {
        return new MessageDataProvider.MessageDTO(
                "1", "sessionId", role, content,
                null, null, null, null, null, null, null, null
        );
    }

    @Nested
    class GenerateTests {

        @Test
        void evaluationNotFound_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.generate(EVALUATION_ID, EXECUTION_SESSION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void benchmarkSessionIdNull_shouldThrow() {
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(createEvaluation(null));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.generate(EVALUATION_ID, EXECUTION_SESSION_ID));
            assertEquals(ErrorCode.EVALUATION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void normalGeneration_shouldInsertResult() throws Exception {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createMessage("user", "hello")));
            when(messageDataProvider.getMessages(String.valueOf(EXECUTION_SESSION_ID)))
                    .thenReturn(List.of(createMessage("assistant", "hi there")));
            ModelConfigData configData = new ModelConfigData("id", "key", "url", "model", 0.5, 100, "platform");
            when(chatDataProvider.getModelConfig(String.valueOf(MODEL_ID))).thenReturn(configData);
            when(modelInvokerManager.getInvoker(configData)).thenReturn(modelInvoker);
            ChatResponse response = new ChatResponse();
            response.setContent("评估结果内容");
            when(modelInvoker.invoke(any(ChatRequest.class))).thenReturn(response);

            service.generate(EVALUATION_ID, EXECUTION_SESSION_ID);

            verify(evaluationResultMapper).insert(resultCaptor.capture());
            EvaluationResult captured = resultCaptor.getValue();
            assertEquals(EVALUATION_ID, captured.getEvaluationId());
            assertEquals(EXECUTION_SESSION_ID, captured.getEvaluationSessionId());
            assertEquals("评估结果内容", captured.getResult());
            assertEquals("COMPLETED", captured.getExecutionStatus());
        }

        @Test
        void modelConfigNotFound_shouldThrow() {
            Evaluation evaluation = createEvaluation(BENCHMARK_SESSION_ID);
            when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
            when(messageDataProvider.getMessages(String.valueOf(BENCHMARK_SESSION_ID)))
                    .thenReturn(List.of(createMessage("user", "hello")));
            when(messageDataProvider.getMessages(String.valueOf(EXECUTION_SESSION_ID)))
                    .thenReturn(List.of(createMessage("assistant", "hi")));
            when(chatDataProvider.getModelConfig(String.valueOf(MODEL_ID))).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.generate(EVALUATION_ID, EXECUTION_SESSION_ID));
            assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
        }
    }
}
