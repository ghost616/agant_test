package com.ghost616.platform.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfigResponse {

    private String platformType;
    private String defaultBaseUrl;
    private List<String> modelNames;
}
