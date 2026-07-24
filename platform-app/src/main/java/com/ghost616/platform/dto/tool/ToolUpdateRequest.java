package com.ghost616.platform.dto.tool;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.platform.enums.SubToolType;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUpdateRequest {

    @Pattern(regexp = "^(?!_sys_)[a-z0-9_]+$", message = "工具名称不允许以 _sys_ 开头")
    private String name;

    private ToolType toolType;

    private String description;

    private String parameterSchema;

    private String returnSchema;

    private String implPath;

    private String authConfig;

    private String toolScript;

    private SubToolType subToolType;

    private CommonStatus status;
}
