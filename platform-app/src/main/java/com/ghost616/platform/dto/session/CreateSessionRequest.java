package com.ghost616.platform.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotNull(message = "agentId不能为空")
    private Long agentId;

    @NotNull(message = "modelId不能为空")
    private Long modelId;

    @NotBlank(message = "title不能为空")
    private String title;
}
