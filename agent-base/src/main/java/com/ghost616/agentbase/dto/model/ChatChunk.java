package com.ghost616.agentbase.dto.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatChunk {

    private String delta;

    private String reasoning;

    private List<ToolCallDelta> toolCalls;

    private String finishReason;

    private Boolean hasToolCalls;

    private Integer index;

    private UsageInfo usage;

    /** 响应 ID（Responses API 流式事件 response.completed 携带，供有状态续接使用） */
    private String responseId;

    /** 网络搜索调用（Responses API output item，非空时携带） */
    private WebSearchCall webSearchCall;

    /** 自定义工具调用（Responses API output item，非空时携带） */
    private CustomToolCall customToolCall;

    /**
     * 网络搜索调用 DTO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSearchCall {

        /** 搜索调用项 ID */
        private String itemId;

        /** 输出项索引 */
        private Integer outputIndex;

        /** 搜索结果列表 */
        private List<WebSearchResult> results;

        /**
         * 搜索结果 DTO。
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WebSearchResult {

            /** 结果标题 */
            private String title;

            /** 结果链接 */
            private String url;

            /** 结果摘要 */
            private String snippet;
        }
    }

    /**
     * 自定义工具调用 DTO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomToolCall {

        /** 工具调用项 ID */
        private String itemId;

        /** 输出项索引 */
        private Integer outputIndex;

        /** 工具输入（JSON 字符串） */
        private String input;

        /** 工具输出（JSON 字符串） */
        private String output;
    }
}
