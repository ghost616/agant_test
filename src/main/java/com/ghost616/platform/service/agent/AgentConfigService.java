package com.ghost616.platform.service.agent;

import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.enums.CommonStatus;

import java.util.List;

public interface AgentConfigService {

    List<AgentConfigDTO> list(String name, CommonStatus status);

    AgentConfigDTO getById(Long id);

    AgentConfigDTO create(AgentCreateRequest request);

    AgentConfigDTO update(Long id, AgentUpdateRequest request);

    void delete(Long id);

    AgentConfigDTO toggleStatus(Long id, CommonStatus status);
}
