package com.ghost616.platform.service.skill;

import com.ghost616.platform.dto.skill.SkillCreateRequest;
import com.ghost616.platform.dto.skill.SkillUpdateRequest;

import java.util.List;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;


public interface SkillConfigService {

    List<SkillConfigDTO> list(String name, CommonStatus status);

    SkillConfigDTO getById(Long id);

    SkillConfigDTO create(SkillCreateRequest request);

    SkillConfigDTO update(Long id, SkillUpdateRequest request);

    void delete(Long id);

    SkillConfigDTO toggleStatus(Long id, CommonStatus status);
}
