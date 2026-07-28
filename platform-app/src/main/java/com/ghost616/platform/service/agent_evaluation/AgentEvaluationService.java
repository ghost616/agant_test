package com.ghost616.platform.service.agent_evaluation;

import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationCreateRequest;
import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationDTO;
import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationUpdateRequest;

import java.util.List;

public interface AgentEvaluationService {

    List<AgentEvaluationDTO> list();

    AgentEvaluationDTO getById(Long id);

    AgentEvaluationDTO create(AgentEvaluationCreateRequest request);

    AgentEvaluationDTO update(Long id, AgentEvaluationUpdateRequest request);

    void delete(Long id);
}
