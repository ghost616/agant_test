package com.ghost616.platform.service.evaluation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.evaluation.EvaluationCreateRequest;
import com.ghost616.platform.dto.evaluation.EvaluationDTO;
import com.ghost616.platform.dto.evaluation.EvaluationResultDTO;
import com.ghost616.platform.dto.evaluation.EvaluationUpdateRequest;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.AgentEvaluation;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.AgentTool;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.entity.Evaluation;
import com.ghost616.platform.entity.EvaluationResult;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionSkill;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.entity.SessionVariable;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.AgentEvaluationMapper;
import com.ghost616.platform.repository.EvaluationMapper;
import com.ghost616.platform.repository.EvaluationResultMapper;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.repository.MessageToolCallMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionSkillMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.repository.SessionVariableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final EvaluationResultMapper evaluationResultMapper;
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final MessageToolCallMapper messageToolCallMapper;
    private final SessionVariableMapper sessionVariableMapper;
    private final SessionToolMapper sessionToolMapper;
    private final SessionSkillMapper sessionSkillMapper;
    private final AgentEvaluationMapper agentEvaluationMapper;
    private final AgentConfigMapper agentConfigMapper;
    private final AgentToolMapper agentToolMapper;
    private final AgentSkillMapper agentSkillMapper;

    @Override
    public List<EvaluationDTO> list(Long agentEvalId) {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        if (agentEvalId != null) {
            wrapper.eq(Evaluation::getAgentEvalId, agentEvalId);
        }
        wrapper.orderByDesc(Evaluation::getCreateTime);
        List<Evaluation> entities = evaluationMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public EvaluationDTO getById(Long id) {
        Evaluation entity = evaluationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public EvaluationDTO create(EvaluationCreateRequest request) {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluation::getName, request.getName());
        if (evaluationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.EVALUATION_ALREADY_EXISTS);
        }

        AgentEvaluation agentEval = agentEvaluationMapper.selectById(request.getAgentEvalId());
        if (agentEval == null) {
            throw new BusinessException(ErrorCode.AGENT_EVALUATION_NOT_FOUND);
        }

        AgentConfig agentConfig = agentConfigMapper.selectById(agentEval.getAgentId());
        String systemPrompt = agentConfig != null ? agentConfig.getSystemPrompt() : null;

        Session baselineSession = new Session();
        baselineSession.setTitle(request.getName() + "BenchmarkSession");
        baselineSession.setModelId(request.getModelId());
        baselineSession.setIsEvaluation(true);
        baselineSession.setAgentId(agentEval.getAgentId());
        baselineSession.setSystemPrompt(systemPrompt);
        sessionMapper.insert(baselineSession);

        Long sessionId = baselineSession.getId();
        Long agentId = agentEval.getAgentId();

        LambdaQueryWrapper<AgentTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(AgentTool::getAgentId, agentId);
        List<AgentTool> agentTools = agentToolMapper.selectList(toolWrapper);
        for (AgentTool agentTool : agentTools) {
            SessionTool sessionTool = new SessionTool();
            sessionTool.setSessionId(sessionId);
            sessionTool.setToolId(agentTool.getToolId());
            sessionTool.setSessionAuth(agentTool.getSessionAuth());
            sessionToolMapper.insert(sessionTool);
        }

        LambdaQueryWrapper<AgentSkill> skillWrapper = new LambdaQueryWrapper<>();
        skillWrapper.eq(AgentSkill::getAgentId, agentId);
        List<AgentSkill> agentSkills = agentSkillMapper.selectList(skillWrapper);
        for (AgentSkill agentSkill : agentSkills) {
            SessionSkill sessionSkill = new SessionSkill();
            sessionSkill.setSessionId(sessionId);
            sessionSkill.setSkillId(agentSkill.getSkillId());
            sessionSkill.setSessionAuth(agentSkill.getSessionAuth());
            sessionSkillMapper.insert(sessionSkill);
        }

        Evaluation entity = new Evaluation();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setBenchmarkSessionId(baselineSession.getId());
        entity.setExecutionCount(request.getExecutionCount());
        entity.setModelId(request.getModelId());
        entity.setAgentEvalId(request.getAgentEvalId());
        entity.setAgentId(agentEval.getAgentId());

        String executionType = request.getExecutionType();
        entity.setExecutionType(executionType != null ? executionType : "BACKGROUND");
        evaluationMapper.insert(entity);

        return toDTO(entity);
    }

    @Override
    @Transactional
    public EvaluationDTO update(Long id, EvaluationUpdateRequest request) {
        Evaluation entity = evaluationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        if (request.getName() != null) {
            if (!request.getName().equals(entity.getName())) {
                LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Evaluation::getName, request.getName());
                if (evaluationMapper.selectCount(wrapper) > 0) {
                    throw new BusinessException(ErrorCode.EVALUATION_ALREADY_EXISTS);
                }
                entity.setName(request.getName());
            }
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getModelId() != null) {
            entity.setModelId(request.getModelId());
        }
        if (request.getExecutionCount() != null) {
            entity.setExecutionCount(request.getExecutionCount());
        }
        if (request.getExecutionType() != null) {
            entity.setExecutionType(request.getExecutionType());
        }

        evaluationMapper.updateById(entity);

        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Evaluation entity = evaluationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        Long benchmarkSessionId = entity.getBenchmarkSessionId();

        LambdaQueryWrapper<EvaluationResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.eq(EvaluationResult::getEvaluationId, id);
        List<EvaluationResult> results = evaluationResultMapper.selectList(resultWrapper);

        for (EvaluationResult result : results) {
            Long sessionId = result.getEvaluationSessionId();

            sessionVariableMapper.delete(new LambdaQueryWrapper<SessionVariable>()
                    .eq(SessionVariable::getSessionId, sessionId));

            sessionToolMapper.delete(new LambdaQueryWrapper<SessionTool>()
                    .eq(SessionTool::getSessionId, sessionId));

            sessionSkillMapper.delete(new LambdaQueryWrapper<SessionSkill>()
                    .eq(SessionSkill::getSessionId, sessionId));

            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getSessionId, sessionId);
            List<Message> messages = messageMapper.selectList(msgWrapper);
            if (!messages.isEmpty()) {
                List<Long> messageIds = messages.stream().map(Message::getId).toList();
                messageToolCallMapper.deleteByMessageIds(messageIds);
                messageMapper.delete(msgWrapper);
            }

            sessionMapper.deleteById(sessionId);
        }

        evaluationResultMapper.delete(resultWrapper);

        if (benchmarkSessionId != null) {
            sessionVariableMapper.delete(new LambdaQueryWrapper<SessionVariable>()
                    .eq(SessionVariable::getSessionId, benchmarkSessionId));
            sessionToolMapper.delete(new LambdaQueryWrapper<SessionTool>()
                    .eq(SessionTool::getSessionId, benchmarkSessionId));
            sessionSkillMapper.delete(new LambdaQueryWrapper<SessionSkill>()
                    .eq(SessionSkill::getSessionId, benchmarkSessionId));

            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getSessionId, benchmarkSessionId);
            List<Message> messages = messageMapper.selectList(msgWrapper);
            if (!messages.isEmpty()) {
                List<Long> messageIds = messages.stream().map(Message::getId).toList();
                messageToolCallMapper.deleteByMessageIds(messageIds);
                messageMapper.delete(msgWrapper);
            }

            sessionMapper.deleteById(benchmarkSessionId);
        }

        evaluationMapper.deleteById(id);
    }

    @Override
    public List<EvaluationResultDTO> listResults(Long evaluationId) {
        LambdaQueryWrapper<EvaluationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EvaluationResult::getEvaluationId, evaluationId);
        wrapper.orderByDesc(EvaluationResult::getCreateTime);
        List<EvaluationResult> entities = evaluationResultMapper.selectList(wrapper);

        List<Long> sessionIds = entities.stream()
                .map(EvaluationResult::getEvaluationSessionId)
                .distinct()
                .toList();
        Map<Long, Session> sessionMap = sessionMapper.selectBatchIds(sessionIds)
                .stream()
                .collect(Collectors.toMap(Session::getId, s -> s));

        return entities.stream()
                .map(e -> toResultDTO(e, sessionMap.get(e.getEvaluationSessionId())))
                .toList();
    }

    private EvaluationDTO toDTO(Evaluation entity) {
        String agentName = null;
        if (entity.getAgentId() != null) {
            AgentConfig agentConfig = agentConfigMapper.selectById(entity.getAgentId());
            if (agentConfig != null) {
                agentName = agentConfig.getName();
            }
        }

        return EvaluationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .executionCount(entity.getExecutionCount())
                .modelId(entity.getModelId())
                .agentEvalId(entity.getAgentEvalId())
                .agentId(entity.getAgentId())
                .agentName(agentName)
                .benchmarkSessionId(entity.getBenchmarkSessionId())
                .executionType(entity.getExecutionType())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private EvaluationResultDTO toResultDTO(EvaluationResult entity, Session session) {
        return EvaluationResultDTO.builder()
                .id(entity.getId())
                .evaluationId(entity.getEvaluationId())
                .evaluationSessionId(entity.getEvaluationSessionId())
                .result(entity.getResult())
                .totalTokenUsed(session != null ? session.getTotalTokenUsed() : null)
                .executionStatus(entity.getExecutionStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
