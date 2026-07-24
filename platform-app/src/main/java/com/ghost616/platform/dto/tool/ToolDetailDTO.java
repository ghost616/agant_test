package com.ghost616.platform.dto.tool;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.platform.enums.SubToolType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ToolDetailDTO extends ToolConfigDTO {

    private String toolScript;

    private SubToolType subToolType;
}
