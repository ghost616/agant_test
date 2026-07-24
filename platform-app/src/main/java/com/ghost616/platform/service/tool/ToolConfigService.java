package com.ghost616.platform.service.tool;

import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;

import java.util.List;

import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;


public interface ToolConfigService {

    List<ToolDetailDTO> list(String name, ToolType toolType, CommonStatus status);

    ToolDetailDTO getById(Long id);

    ToolDetailDTO create(ToolCreateRequest request);

    ToolDetailDTO update(Long id, ToolUpdateRequest request);

    void delete(Long id);

    ToolDetailDTO toggleStatus(Long id, CommonStatus status);

    ToolDetailDTO getImplByName(String name);
}
