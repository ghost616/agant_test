package com.ghost616.agentbase.service.agent.invoker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;

public class HookManager {

    private static final Logger log = LoggerFactory.getLogger(HookManager.class);

    private final Map<HookPhase, List<HookInvoker>> systemHooks = new HashMap<>();
    private final List<HookInvoker> systemPostHooks = new ArrayList<>();
    private final Map<HookPhase, List<HookInvoker>> regularPhaseHooks = new HashMap<>();

    public void refreshHooks(List<HookInvoker> hooks) {
        systemHooks.clear();
        systemPostHooks.clear();
        regularPhaseHooks.clear();
        for (HookInvoker hook : hooks) {
            HookPhase phase = hook.getPhase();
            if (hook instanceof SystemPostHook) {
                systemPostHooks.add(hook);
            } else if (hook instanceof SystemHook) {
                systemHooks.computeIfAbsent(phase, k -> new ArrayList<>()).add(hook);
            } else {
                regularPhaseHooks.computeIfAbsent(phase, k -> new ArrayList<>()).add(hook);
            }
        }
    }

    public void triggerHooks(HookPhase phase, AgentExecutionContext ctx, HookData data) {
        List<HookInvoker> regularHooks = regularPhaseHooks.get(phase);
        if (regularHooks != null) {
            regularHooks.forEach(h -> {
                try {
                    h.execute(ctx, data);
                } catch (Exception e) {
                    log.warn("Hook execution failed for {}", h.getClass().getName(), e);
                }
            });
        }
        List<HookInvoker> hooks = systemHooks.get(phase);
        if (hooks != null) {
            hooks.stream()
                    .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                    .forEach(h -> {
                        try {
                            h.execute(ctx, data);
                        } catch (Exception e) {
                            log.warn("Hook execution failed for {}", h.getClass().getName(), e);
                        }
                    });
        }
    }

    public void executePostHooks(AgentExecutionContext ctx, HookData data) {
        systemPostHooks.stream()
                .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                .forEach(h -> {
                    try {
                        h.execute(ctx, data);
                    } catch (Exception e) {
                        log.warn("Hook execution failed for {}", h.getClass().getName(), e);
                    }
                });
    }
}
