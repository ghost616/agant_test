package com.ghost616.agentbase.core;

/**
 * 线程变量包装器，承载在提交任务的线程捕获的线程变量快照。
 * <p>
 * 由 {@link ThreadVariableHandler#wrap()} 在提交任务的线程创建，
 * 传入异步线程后通过 {@link #apply()} 将捕获的线程变量赋值到当前线程，
 * 实现线程变量在异步执行点之间的传播。
 */
public interface ThreadVariableWrapper {

    /**
     * 将捕获的线程变量赋值到当前线程。
     */
    void apply();
}
