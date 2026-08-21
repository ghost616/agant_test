package com.ghost616.agentinteg.subsession;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.FinishReason;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.HookData;
import com.ghost616.agentbase.service.agent.invoker.SystemPostHook;

/**
 * 子会话结果兜底回传 HOOK。
 *
 * <p>在流式回复完成（AFTER_MESSAGE_RECEIVE 阶段、最终回复无工具调用）时，
 * 通过 {@link SubSessionResultProvider} 判断是否需要向父会话兜底回传执行结果；
 * 需要时取子会话最后一条 assistant 消息内容，经 {@code ctx.sendParentMessage(content)}
 * 发送到父会话（复用既有 sendParentMessage 通道）。</p>
 *
 * <p>非子会话、已有待执行工具调用、Provider 判定无需发送等情况一律静默跳过，
 * 不影响原有流程。</p>
 */
@Slf4j
public class SubSessionResultFallbackHook implements SystemPostHook {

    /** 消息保存 HOOK 之后执行的索引，保证从上下文历史中能取到本次最终 assistant 消息 */
    private static final int POST_SAVE_INDEX = 100;

    private final SubSessionResultProvider resultProvider;

    public SubSessionResultFallbackHook(SubSessionResultProvider resultProvider) {
        this.resultProvider = resultProvider;
    }

    @Override
    public HookPhase getPhase() {
        return HookPhase.AFTER_MESSAGE_RECEIVE;
    }

    @Override
    public int getIndex() {
        return POST_SAVE_INDEX;
    }

    @Override
    public void execute(AgentExecutionContext ctx, HookData data) {
        if (ctx == null || ctx.isMainSession()) {
            return;
        }
        if (data == null) {
            return;
        }
        ChatChunk chunk = data.getChatChunk();
        if (chunk == null) {
            return;
        }
        // 已有待执行工具调用：本轮为工具调用轮，最终回复尚未完成，跳过
        if (Boolean.TRUE.equals(chunk.getHasToolCalls())) {
            return;
        }
        // 仅流式回复完成（STOP）时兜底回传；普通内容 chunk 与完成标志 chunk 均跳过，避免重复发送
        if (!FinishReason.STOP.equals(chunk.getFinishReason())) {
            return;
        }
        String sessionId = ctx.getSessionId();
        if (!resultProvider.shouldSendResultToParent(sessionId)) {
            log.debug("子会话 {} 无需向父会话回传执行结果", sessionId);
            return;
        }
        String content = findLastAssistantContent(ctx.getHistory());
        if (content == null || content.isBlank()) {
            log.debug("子会话 {} 无可用 assistant 消息内容，跳过回传", sessionId);
            return;
        }
        ctx.sendParentMessage(content);
        log.info("子会话 {} 兜底回传执行结果到父会话", sessionId);
    }

    /**
     * 从会话历史中查找最后一条 assistant 消息内容。
     *
     * @param history 会话历史（可为 null）
     * @return 最后一条 assistant 消息的 content；无 assistant 消息时返回 null
     */
    private String findLastAssistantContent(List<AgentExecutionContext.HistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            AgentExecutionContext.HistoryEntry entry = history.get(i);
            if (entry != null && "assistant".equals(entry.role())) {
                return entry.content();
            }
        }
        return null;
    }
}
