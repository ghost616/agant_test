package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.service.agent.AgentMessageProxy;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncEvaluationExecutorTest {

    @Mock
    private ChatService chatService;
    @Mock
    private ToolExecutionService toolExecutionService;
    @Mock
    private EvaluationResultGenerateService evaluationResultGenerateService;

    private AsyncEvaluationExecutor executor;

    private static final Long EVALUATION_ID = 1L;
    private static final Long EXECUTION_SESSION_ID = 200L;
    private static final String STATUS_KEY = "1:200";

    @BeforeEach
    void setUp() {
        executor = new AsyncEvaluationExecutor(
                chatService, toolExecutionService, evaluationResultGenerateService);
    }

    @Nested
    class GenerateResultAsyncTests {

        @Test
        void successfulGenerate_shouldUpdateStatusToCompleted() {
            Map<String, EvaluationExecutionStatusDTO> statusMap = new ConcurrentHashMap<>();

            executor.generateResultAsync(EVALUATION_ID, EXECUTION_SESSION_ID, statusMap);

            verify(evaluationResultGenerateService).generate(EVALUATION_ID, EXECUTION_SESSION_ID);
            EvaluationExecutionStatusDTO status = statusMap.get(STATUS_KEY);
            assertNotNull(status);
            assertEquals(EVALUATION_ID, status.getEvaluationId());
            assertEquals(EXECUTION_SESSION_ID, status.getExecutionSessionId());
            assertEquals("COMPLETED", status.getStatus());
            assertEquals(1, status.getCurrentStep());
            assertEquals(1, status.getTotalSteps());
        }

        @Test
        void generateThrows_shouldUpdateStatusToFailed() {
            doThrow(new RuntimeException("generate failed"))
                    .when(evaluationResultGenerateService).generate(EVALUATION_ID, EXECUTION_SESSION_ID);
            Map<String, EvaluationExecutionStatusDTO> statusMap = new ConcurrentHashMap<>();

            executor.generateResultAsync(EVALUATION_ID, EXECUTION_SESSION_ID, statusMap);

            verify(evaluationResultGenerateService).generate(EVALUATION_ID, EXECUTION_SESSION_ID);
            EvaluationExecutionStatusDTO status = statusMap.get(STATUS_KEY);
            assertNotNull(status);
            assertEquals(EVALUATION_ID, status.getEvaluationId());
            assertEquals(EXECUTION_SESSION_ID, status.getExecutionSessionId());
            assertEquals("FAILED", status.getStatus());
            assertEquals(1, status.getCurrentStep());
            assertEquals(1, status.getTotalSteps());
        }
    }

    @Nested
    class ExecuteAsyncTests {

        @Test
        void successfulExecution_shouldUpdateStatusToCompleted() {
            Map<String, EvaluationExecutionStatusDTO> statusMap = new ConcurrentHashMap<>();

            executor.executeAsync(EVALUATION_ID, EXECUTION_SESSION_ID, java.util.List.of(), statusMap);

            EvaluationExecutionStatusDTO status = statusMap.get(String.valueOf(EVALUATION_ID));
            assertNotNull(status);
            assertEquals("COMPLETED", status.getStatus());
            assertEquals(EVALUATION_ID, status.getEvaluationId());
        }
    }
}
