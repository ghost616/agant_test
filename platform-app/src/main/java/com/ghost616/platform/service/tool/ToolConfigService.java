package com.ghost616.platform.service.tool;

import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;

import java.util.List;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;


public interface ToolConfigService {

    List<ToolConfigDTO> list(String name, ToolType toolType, CommonStatus status);

    ToolConfigDTO getById(Long id);

    ToolConfigDTO create(ToolCreateRequest request);

    ToolConfigDTO update(Long id, ToolUpdateRequest request);

    void delete(Long id);

    ToolConfigDTO toggleStatus(Long id, CommonStatus status);

    ToolConfigDTO getImplByName(String name);
}
