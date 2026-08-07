package com.ghost616.agentinteg.knowledge;

/**
 * 知识库文件信息数据类。
 */
public record FileInfo(Long fileId, String fileName, String fileDescription, int maxLineCount) {
}
