package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.core.ThreadVariableWrapper;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.service.agent.AgentMessageProxy;
import com.ghost616.agentbase.service.agent.ChatDataCacheManager;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.session.UserContext;
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
    private final ChatDataCacheManager chatDataCacheManager;

    /**
     * 异步生成评估结果。任务提交方（{@link EvaluationExecutionService}）通过
     * {@link ThreadVariableWrapper} 传播当前登录用户，此处先恢复用户上下文，
     * 执行结束后在 finally 中清理，避免线程复用导致会话串号。
     *
     * @param evaluationId          评估 ID
     * @param executionSessionId    执行会话 ID
     * @param generateStatusMap     结果生成状态缓存
     * @param threadVariableWrapper 线程变量包装器（提交线程捕获的用户上下文快照，可为 null）
     */
    @Async
    public void generateResultAsync(Long evaluationId, Long executionSessionId,
                                    Map<String, EvaluationExecutionStatusDTO> generateStatusMap,
                                    ThreadVariableWrapper threadVariableWrapper) {
        if (threadVariableWrapper != null) {
            threadVariableWrapper.apply();
        }
        try {
            String statusKey = evaluationId + ":" + executionSessionId;
            try {
                evaluationResultGenerateService.generate(evaluationId, executionSessionId);
                generateStatusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                        .evaluationId(evaluationId)
                        .executionSessionId(executionSessionId)
                        .status("COMPLETED")
                        .currentStep(1)
                        .totalSteps(1)
                        .build());
            } catch (Exception e) {
                log.error("评估结果生成异常, evaluationId={}, executionSessionId={}", evaluationId, executionSessionId, e);
                generateStatusMap.put(statusKey, EvaluationExecutionStatusDTO.builder()
                        .evaluationId(evaluationId)
                        .executionSessionId(executionSessionId)
                        .status("FAILED")
                        .currentStep(1)
                        .totalSteps(1)
                        .build());
            }
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 异步执行评估：逐条发送基准用户消息并生成评估结果。任务提交方通过
     * {@link ThreadVariableWrapper} 传播当前登录用户，此处先恢复用户上下文，
     * 执行结束后在 finally 中清理。
     *
     * @param evaluationId          评估 ID
     * @param executionSession      执行会话
     * @param userMessages          基准会话用户消息列表
     * @param statusMap             执行状态缓存
     * @param threadVariableWrapper 线程变量包装器（提交线程捕获的用户上下文快照，可为 null）
     */
    @Async
    public void executeAsync(Long evaluationId, Session executionSession,
                             List<MessageDataProvider.MessageDTO> userMessages,
                             Map<String, EvaluationExecutionStatusDTO> statusMap,
                             ThreadVariableWrapper threadVariableWrapper) {
        if (threadVariableWrapper != null) {
            threadVariableWrapper.apply();
        }
        try {
            String statusKey = String.valueOf(evaluationId);
            AgentMessageProxy proxy = new AgentMessageProxy(chatService, toolExecutionService);
            proxy.setChatDataCacheManager(chatDataCacheManager);
            Long executionSessionId = executionSession.getId();

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
                        Message response = proxy.sendUserMessageToSession(
                                String.valueOf(executionSessionId),
                                content,
                                null,
                                executionSession.getThinking());
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
        } finally {
            UserContext.clear();
        }
    }
}
