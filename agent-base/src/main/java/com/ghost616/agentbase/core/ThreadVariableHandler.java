package com.ghost616.agentbase.core;

/**
 * 线程变量处理器，负责在提交任务的线程捕获当前线程变量。
 * <p>
 * 供异步执行点（如工具执行的 {@code CompletableFuture.supplyAsync}）使用：
 * 提交任务前调用 {@link #wrap()} 捕获当前线程变量，异步线程开始执行时
 * 调用 {@link ThreadVariableWrapper#apply()} 恢复线程变量。
 */
public interface ThreadVariableHandler {

    /**
     * 捕获当前线程的线程变量，返回可在异步线程中恢复的快照。
     *
     * @return 线程变量快照，非 null
     */
    ThreadVariableWrapper wrap();
}
