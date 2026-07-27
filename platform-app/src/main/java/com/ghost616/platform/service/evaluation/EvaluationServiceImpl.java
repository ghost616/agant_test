package com.ghost616.platform.service.evaluation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.dto.evaluation.EvaluationCreateRequest;
import com.ghost616.platform.dto.evaluation.EvaluationDTO;
import com.ghost616.platform.dto.evaluation.EvaluationUpdateRequest;
import com.ghost616.platform.entity.Evaluation;
import com.ghost616.platform.entity.EvaluationResult;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionSkill;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.entity.SessionVariable;
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

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;

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

    @Override
    public List<EvaluationDTO> list() {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
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

        Evaluation entity = new Evaluation();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setBenchmarkSessionId(request.getBenchmarkSessionId());
        entity.setExecutionCount(request.getExecutionCount());
        entity.setModelId(request.getModelId());
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
        if (request.getBenchmarkSessionId() != null) {
            entity.setBenchmarkSessionId(request.getBenchmarkSessionId());
        }
        if (request.getExecutionCount() != null) {
            entity.setExecutionCount(request.getExecutionCount());
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

        evaluationMapper.deleteById(id);
    }

    private EvaluationDTO toDTO(Evaluation entity) {
        return EvaluationDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .benchmarkSessionId(entity.getBenchmarkSessionId())
                .executionCount(entity.getExecutionCount())
                .modelId(entity.getModelId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
