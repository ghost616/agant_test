package com.ghost616.agentbase.dto.tool;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
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
    private SessionAuthType sessionAuth;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
