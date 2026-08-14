package com.ghost616.agentinteg.memory;

/**
 * AI 记忆查询结果数据类，包含记忆内容、序号区间与记忆类型。
 */
public record MemoryResult(String content, int startSeq, int endSeq, String memoryType) {
}
