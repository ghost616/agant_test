package com.ghost616.platform.dto;

import lombok.Data;

@Data
public class ToolStatusResultDTO {

    @Data
    public static class ToolConfigBrief {
        private String id;
        private String subToolType;
        private String toolName;
    }

    private String status;
    private String toolId;
    private String toolName;
    private String arguments;
    private boolean hasMore;
    private String result;
    private String message;
    private boolean needsSubSessionFlow;
    private ToolConfigBrief toolConfig;

    public ToolStatusResultDTO() {
    }

    public ToolStatusResultDTO(String status, String toolId, String toolName, String arguments,
                                boolean hasMore, String result, String message, boolean needsSubSessionFlow) {
        this(status, toolId, toolName, arguments, hasMore, result, message, needsSubSessionFlow, null);
    }

    public ToolStatusResultDTO(String status, String toolId, String toolName, String arguments,
                                boolean hasMore, String result, String message, boolean needsSubSessionFlow,
                                ToolConfigBrief toolConfig) {
        this.status = status;
        this.toolId = toolId;
        this.toolName = toolName;
        this.arguments = arguments;
        this.hasMore = hasMore;
        this.result = result;
        this.message = message;
        this.needsSubSessionFlow = needsSubSessionFlow;
        this.toolConfig = toolConfig;
    }
}
