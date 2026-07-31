package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.service.agent.AgentMessageProxy;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEvaluationExecutor {

    private final ChatService chatService;
    private final ToolExecutionService toolExecutionService;
    private final EvaluationResultGenerateService evaluationResultGenerateService;

    @Async
    public void executeAsync(Long evaluationId, Long executionSessionId,
                             List<MessageDataProvider.MessageDTO> userMessages,
                             Map<String, EvaluationExecutionStatusDTO> statusMap) {
        String statusKey = String.valueOf(evaluationId);
        AgentMessageProxy proxy = new AgentMessageProxy(chatService, toolExecutionService);

        try {
            int total = userMessages.size();
            for (int i = 0; i < total; i++) {
                MessageDataProvider.MessageDTO userMsg = userMessages.get(i);
                String content = userMsg.content() != null ? userMsg.content() : "";

                statusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                        .evaluationId(evaluationId)
                        .executionSessionId(executionSessionId)
                        .status("RUNNING")
                        .currentStep(i + 1)
                        .totalSteps(total)
                        .build());

                try {
                    Message response = proxy.sendUserMessage(
                            String.valueOf(executionSessionId),
                            content,
                            null,
                            null);
                    log.debug("评估执行消息处理完成, sessionId={}, step={}/{}", executionSessionId, i + 1, total);
                } catch (Exception e) {
                    log.error("评估执行用户消息处理失败, sessionId={}, step={}/{}", executionSessionId, i + 1, total, e);
                    statusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                            .evaluationId(evaluationId)
                            .executionSessionId(executionSessionId)
                            .status("FAILED")
                            .currentStep(i + 1)
                            .totalSteps(total)
                            .build());
                    return;
                }
            }

            evaluationResultGenerateService.generate(evaluationId, executionSessionId);

            statusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                    .evaluationId(evaluationId)
                    .executionSessionId(executionSessionId)
                    .status("COMPLETED")
                    .currentStep(total)
                    .totalSteps(total)
                    .build());

        } catch (Exception e) {
            log.error("评估执行异常, evaluationId={}", evaluationId, e);
            statusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                    .evaluationId(evaluationId)
                    .executionSessionId(executionSessionId)
                    .status("FAILED")
                    .build());
        }
    }
}
