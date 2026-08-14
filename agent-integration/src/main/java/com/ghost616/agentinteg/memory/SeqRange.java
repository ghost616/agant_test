package com.ghost616.agentinteg.memory;

/**
 * 消息序号区间，封装起始与结束序号。
 */
public record SeqRange(int startSeq, int endSeq) {
}
