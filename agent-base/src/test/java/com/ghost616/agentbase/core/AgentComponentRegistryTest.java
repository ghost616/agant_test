package com.ghost616.agentbase.core;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.CacheSessionInfo;
import com.ghost616.agentbase.service.agent.ChatDataCacheManager;
import com.ghost616.agentbase.service.agent.ChatDataCacheProvider;
import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.service.agent.log.LogData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentComponentRegistryTest {

    private AgentComponentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void getAgentLog未设置时应直接返回null() {
        assertNull(registry.getAgentLog());
    }

    @Test
    void setAgentLog后getAgentLog应返回设置实例() {
        AgentLog agentLog = new AgentLog() {
            @Override
            public void addLog(LogData logData) {
            }
        };
        registry.setAgentLog(agentLog);
        assertSame(agentLog, registry.getAgentLog());
    }

    @Test
    void getChatDataCacheManager未设置时应直接返回null() {
        assertNull(registry.getChatDataCacheManager());
    }

    @Test
    void setChatDataCacheManager后getChatDataCacheManager应返回设置实例() {
        ChatDataCacheManager manager = new ChatDataCacheManager(new ChatDataCacheProvider() {
            @Override
            public String createCache(String sessionId, String conversationId) {
                return null;
            }

            @Override
            public boolean cacheExists(String cacheId) {
                return false;
            }

            @Override
            public boolean cacheExists(String sessionId, String conversationId) {
                return false;
            }

            @Override
            public boolean isCacheDone(String cacheId) {
                return false;
            }

            @Override
            public String getCacheId(String sessionId, String conversationId) {
                return null;
            }

            @Override
            public CacheSessionInfo getCacheSessionInfo(String cacheId) {
                return null;
            }

            @Override
            public int getMaxChunkIndex(String cacheId) {
                return -1;
            }

            @Override
            public void appendChunk(String cacheId, ChatChunk chunk) {
            }

            @Override
            public void removeCache(String cacheId) {
            }

            @Override
            public List<ChatChunk> getChunks(String cacheId, int startIndex, int endIndex) {
                return Collections.emptyList();
            }
        });
        registry.setChatDataCacheManager(manager);
        assertSame(manager, registry.getChatDataCacheManager());
    }

    @Test
    void getThreadVariableHandler未设置时应直接返回null() {
        assertNull(registry.getThreadVariableHandler());
    }

    @Test
    void setThreadVariableHandler后getThreadVariableHandler应返回设置实例() {
        ThreadVariableHandler handler = () -> () -> {
        };
        registry.setThreadVariableHandler(handler);
        assertSame(handler, registry.getThreadVariableHandler());
    }
}
