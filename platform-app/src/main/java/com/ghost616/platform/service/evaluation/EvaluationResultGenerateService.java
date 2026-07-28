package com.ghost616.platform.service.evaluation;

import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.dto.model.Message;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationResultGenerateService {

    private final EvaluationMapper evaluationMapper;
    private final EvaluationResultMapper evaluationResultMapper;
    private final MessageDataProvider messageDataProvider;
    private final ChatDataProvider chatDataProvider;
    private final ModelInvokerManager modelInvokerManager;

    public void generate(Long evaluationId, Long executionSessionId) {
        Evaluation evaluation = evaluationMapper.selectById(evaluationId);
        if (evaluation == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        Long benchmarkSessionId = evaluation.getBenchmarkSessionId();
        if (benchmarkSessionId == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        List<MessageDataProvider.MessageDTO> benchmarkMessages = messageDataProvider.getMessages(
                String.valueOf(benchmarkSessionId));
        List<MessageDataProvider.MessageDTO> executionMessages = messageDataProvider.getMessages(
                String.valueOf(executionSessionId));

        List<Message> judgeMessages = buildJudgeMessages(benchmarkMessages, executionMessages);

        ModelConfigData configData = chatDataProvider.getModelConfig(String.valueOf(evaluation.getModelId()));
        if (configData == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }

        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(judgeMessages)
                .build();

        ChatResponse response;
        try {
            response = invoker.invoke(chatRequest);
        } catch (Exception e) {
            log.error("评估模型调用失败, evaluationId={}", evaluationId, e);
            throw new BusinessException(ErrorCode.EVALUATION_RESULT_GENERATE_ERROR);
        }

        String resultContent = response != null ? response.getContent() : "";

        EvaluationResult evaluationResult = new EvaluationResult();
        evaluationResult.setEvaluationId(evaluationId);
        evaluationResult.setEvaluationSessionId(executionSessionId);
        evaluationResult.setResult(resultContent);
        evaluationResult.setExecutionStatus("COMPLETED");
        evaluationResultMapper.insert(evaluationResult);
    }

    private List<Message> buildJudgeMessages(List<MessageDataProvider.MessageDTO> benchmarkMessages,
                                              List<MessageDataProvider.MessageDTO> executionMessages) {
        List<Message> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        systemContent.append("你是一个评估助手，需要对比基准会话和执行会话的对话内容，评估执行会话的回复质量。\n\n");

        systemContent.append("## 基准会话消息\n");
        if (benchmarkMessages != null && !benchmarkMessages.isEmpty()) {
            for (MessageDataProvider.MessageDTO msg : benchmarkMessages) {
                String role = msg.role() != null ? msg.role() : "unknown";
                String content = msg.content() != null ? msg.content() : "";
                systemContent.append("【").append(role).append("】: ").append(content).append("\n");
            }
        } else {
            systemContent.append("（无基准消息）\n");
        }

        systemContent.append("\n## 执行会话消息\n");
        if (executionMessages != null && !executionMessages.isEmpty()) {
            for (MessageDataProvider.MessageDTO msg : executionMessages) {
                String role = msg.role() != null ? msg.role() : "unknown";
                String content = msg.content() != null ? msg.content() : "";
                systemContent.append("【").append(role).append("】: ").append(content).append("\n");
            }
        } else {
            systemContent.append("（无执行消息）\n");
        }

        systemContent.append("\n请基于以上对比，对执行会话的回复质量进行评估，包括回复的相关性、准确性、完整性等方面，并给出评分和改进建议。");

        messages.add(Message.builder()
                .role("system")
                .content(systemContent.toString())
                .build());

        messages.add(Message.builder()
                .role("user")
                .content("请对执行会话的回复质量进行评估。")
                .build());

        return messages;
    }
}
