package com.ghost616.platform.dto.evaluation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSessionCreateResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    private List<String> userMessages;
}
