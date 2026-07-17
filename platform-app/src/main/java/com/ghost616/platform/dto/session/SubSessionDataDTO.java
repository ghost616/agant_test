package com.ghost616.platform.dto.session;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubSessionDataDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long childSessionId;

    private String userMessage;

    private Boolean thinking;
}
