package com.ghost616.platform.service.model;

import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.dto.model.ModelUpdateRequest;
import com.ghost616.platform.enums.PlatformType;

import java.util.List;

import com.ghost616.agentbase.enums.CommonStatus;


public interface ModelConfigService {

    List<ModelConfigDTO> list(String name, PlatformType platformType, CommonStatus status);

    ModelConfigDTO getById(Long id);

    ModelConfigDTO create(ModelCreateRequest request);

    ModelConfigDTO update(Long id, ModelUpdateRequest request);

    void delete(Long id);

    ModelConfigDTO toggleStatus(Long id, CommonStatus status);
}
