package com.ghost616.agentinteg.memory;

import java.util.List;

/**
 * 消息序号按角色分类的结果数据类，包含 user/tool/assistant 三个序号列表。
 */
public record MessageSeqByRole(List<Integer> userSeqList, List<Integer> toolSeqList,
                               List<Integer> assistantSeqList) {
}
