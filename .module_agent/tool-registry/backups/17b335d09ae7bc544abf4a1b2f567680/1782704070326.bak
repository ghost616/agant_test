package com.ghost616.platform.dto.tool;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.ToolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolConfigDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private ToolType toolType;
    private String description;
    private String parameterSchema;
    private String returnSchema;
    private String implPath;
    private String authConfig;
    private CommonStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
