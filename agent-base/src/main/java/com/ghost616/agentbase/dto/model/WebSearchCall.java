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
public class WebSearchCall {

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
