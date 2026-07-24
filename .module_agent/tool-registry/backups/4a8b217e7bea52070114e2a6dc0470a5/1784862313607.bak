package com.ghost616.platform.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCreateRequest {

    @NotBlank(message = "工具名称不能为空")
    @Pattern(regexp = "^(?!_sys_)[a-z0-9_]+$", message = "工具名称不允许以 _sys_ 开头")
    private String name;

    @NotNull(message = "工具类型不能为空")
    private ToolType toolType;

    private String description;

    private String parameterSchema;

    private String returnSchema;

    @NotBlank(message = "实现路径不能为空")
    private String implPath;

    private String authConfig;

    private CommonStatus status;
}
