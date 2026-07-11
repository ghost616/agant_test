package com.ghost616.platform.service.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;


@ExtendWith(MockitoExtension.class)
class AgentExecutionContextTest {

    private AgentExecutionContext context;
    private AgentExecutionContext.AgentContextMutator mutator;

    @BeforeEach
    void setUp() {
        mutator = new AgentExecutionContext.AgentContextMutator();
        context = new AgentExecutionContext(
                1L, 1L, "system prompt", 1L, 10,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                mutator, new HashMap<>(), new HashMap<>(), null, null);
    }

    @Test
    void isStopped_默认返回false() {
        assertFalse(context.isStopped());
    }

    @Test
    void setStopped_后isStopped返回true() {
        mutator.setStopped();
        assertTrue(context.isStopped());
    }

    @Test
    void resetStopped_后isStopped返回false() {
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }

    @Test
    void resetStopped_未设置时调用仍返回false() {
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }

    @Test
    void setStopped_多次调用仍返回true() {
        mutator.setStopped();
        mutator.setStopped();
        assertTrue(context.isStopped());
    }

    @Test
    void setStopped_resetStopped_可重复切换() {
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }

    private void setPutCallback(BiConsumer<String, String> cb) {
        try {
            Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("conversationVarPutCallback");
            f.setAccessible(true);
            f.set(mutator, cb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setRemoveCallback(Consumer<String> cb) {
        try {
            Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("conversationVarRemoveCallback");
            f.setAccessible(true);
            f.set(mutator, cb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class ConversationVariable功能 {

        @Test
        void 正向_mutator_putConversationVariable_调用回调() {
            AtomicReference<String> capturedKey = new AtomicReference<>();
            AtomicReference<String> capturedValue = new AtomicReference<>();
            setPutCallback((k, v) -> {
                capturedKey.set(k);
                capturedValue.set(v);
            });

            mutator.putConversationVariable("key1", "val1");

            assertEquals("key1", capturedKey.get());
            assertEquals("val1", capturedValue.get());
        }

        @Test
        void 正向_mutator_removeConversationVariable_调用回调() {
            AtomicReference<String> capturedKey = new AtomicReference<>();
            setRemoveCallback(k -> capturedKey.set(k));

            mutator.removeConversationVariable("keyToRemove");

            assertEquals("keyToRemove", capturedKey.get());
        }

        @Test
        void 反向_mutator_putConversationVariable_回调为null时不抛异常() {
            setPutCallback(null);

            assertDoesNotThrow(() -> mutator.putConversationVariable("k", "v"));
        }

        @Test
        void 反向_mutator_removeConversationVariable_回调为null时不抛异常() {
            setRemoveCallback(null);

            assertDoesNotThrow(() -> mutator.removeConversationVariable("k"));
        }

        @Test
        void 边界_conversationVarPutCallback和RemoveCallback可独立设置() {
            AtomicReference<String> putKey = new AtomicReference<>();
            setPutCallback((k, v) -> putKey.set(k));

            mutator.putConversationVariable("onlyPut", "v");
            assertEquals("onlyPut", putKey.get());

            assertDoesNotThrow(() -> mutator.removeConversationVariable("anyKey"));
        }
    }

    @Nested
    class ExecutionContextConversationVariable {

        @Test
        void 正向_putConversationVariable_更新本地map并调用mutator回调() {
            AtomicReference<String> callbackKey = new AtomicReference<>();
            AtomicReference<String> callbackValue = new AtomicReference<>();
            setPutCallback((k, v) -> {
                callbackKey.set(k);
                callbackValue.set(v);
            });

            context.putConversationVariable("ctxKey", "ctxVal");

            assertEquals("ctxKey", callbackKey.get());
            assertEquals("ctxVal", callbackValue.get());
            assertEquals("ctxVal", context.getConversationVariable("ctxKey"));
        }

        @Test
        void 正向_removeConversationVariable_更新本地map并调用mutator回调() {
            context.putConversationVariable("toBeRemoved", "someValue");
            assertNotNull(context.getConversationVariable("toBeRemoved"));

            AtomicReference<String> callbackKey = new AtomicReference<>();
            setRemoveCallback(k -> callbackKey.set(k));

            context.removeConversationVariable("toBeRemoved");

            assertEquals("toBeRemoved", callbackKey.get());
            assertNull(context.getConversationVariable("toBeRemoved"));
        }

        @Test
        void 正向_putConversationVariable_多次写入不同key各自独立() {
            context.putConversationVariable("k1", "v1");
            context.putConversationVariable("k2", "v2");

            assertEquals("v1", context.getConversationVariable("k1"));
            assertEquals("v2", context.getConversationVariable("k2"));
        }

        @Test
        void 正向_putConversationVariable_覆盖已有key() {
            context.putConversationVariable("dup", "old");
            context.putConversationVariable("dup", "new");

            assertEquals("new", context.getConversationVariable("dup"));
        }

        @Test
        void 反向_removeConversationVariable_不存在的key不抛异常() {
            assertDoesNotThrow(() -> context.removeConversationVariable("nonExistent"));
        }

        @Test
        void 边界_putConversationVariable_key为null不抛异常() {
            assertDoesNotThrow(() -> context.putConversationVariable(null, "value"));
        }

        @Test
        void 边界_putConversationVariable_value为null不抛异常() {
            assertDoesNotThrow(() -> context.putConversationVariable("key", null));
        }
    }

    @Nested
    class MutatorSessionVariable {

        private void setGetCallback(Function<String, String> cb) {
            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getSessionVarCallback");
                f.setAccessible(true);
                f.set(mutator, cb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void setGetKeysCallback(Supplier<Set<String>> cb) {
            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getSessionVarKeysCallback");
                f.setAccessible(true);
                f.set(mutator, cb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void 正向_mutator_getSessionVariable_调用回调() {
            setGetCallback(k -> "value_for_" + k);
            assertEquals("value_for_myKey", mutator.getSessionVariable("myKey"));
        }

        @Test
        void 反向_mutator_getSessionVariable_回调为null时不抛异常返回null() {
            setGetCallback(null);
            assertNull(mutator.getSessionVariable("anyKey"));
        }

        @Test
        void 正向_mutator_getSessionVariableKeys_调用回调() {
            setGetKeysCallback(() -> Set.of("k1", "k2"));
            Set<String> keys = mutator.getSessionVariableKeys();
            assertEquals(2, keys.size());
            assertTrue(keys.contains("k1"));
            assertTrue(keys.contains("k2"));
        }

        @Test
        void 反向_mutator_getSessionVariableKeys_回调为null返回空Set() {
            setGetKeysCallback(null);
            assertTrue(mutator.getSessionVariableKeys().isEmpty());
        }
    }

    @Nested
    class MutatorConversationVariable {

        private void setGetCallback(Function<String, String> cb) {
            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getConversationVarCallback");
                f.setAccessible(true);
                f.set(mutator, cb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void setGetKeysCallback(Supplier<Set<String>> cb) {
            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getConversationVarKeysCallback");
                f.setAccessible(true);
                f.set(mutator, cb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void 正向_mutator_getConversationVariable_调用回调() {
            setGetCallback(k -> "conv_" + k);
            assertEquals("conv_myKey", mutator.getConversationVariable("myKey"));
        }

        @Test
        void 反向_mutator_getConversationVariable_回调为null时不抛异常返回null() {
            setGetCallback(null);
            assertNull(mutator.getConversationVariable("anyKey"));
        }

        @Test
        void 正向_mutator_getConversationVariableKeys_调用回调() {
            setGetKeysCallback(() -> Set.of("ck1", "ck2"));
            Set<String> keys = mutator.getConversationVariableKeys();
            assertEquals(2, keys.size());
            assertTrue(keys.contains("ck1"));
            assertTrue(keys.contains("ck2"));
        }

        @Test
        void 反向_mutator_getConversationVariableKeys_回调为null返回空Set() {
            setGetKeysCallback(null);
            assertTrue(mutator.getConversationVariableKeys().isEmpty());
        }
    }

    @Nested
    class SendUserMessageTest {

        private void setCallback(AgentExecutionContext.AgentContextMutator.SendUserMessageCallback cb) {
            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("sendUserMessageCallback");
                f.setAccessible(true);
                f.set(mutator, cb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void 正向_mutator_sendUserMessage_调用回调返回Message() {
            Message expected = Message.builder().role("user").content("hello").build();
            setCallback(new AgentExecutionContext.AgentContextMutator.SendUserMessageCallback() {
                @Override
                public Message send(Long childSessionId, String content, Long modelId) {
                    return expected;
                }
            });

            Message result = mutator.sendUserMessage(1L, "hello", 100L);
            assertNotNull(result);
            assertEquals("user", result.getRole());
            assertEquals("hello", result.getContent());
        }

        @Test
        void 反向_mutator_sendUserMessage_回调为null返回null() {
            assertNull(mutator.sendUserMessage(1L, "hello", 100L));
        }

        @Test
        void 正向_context_sendUserMessage_透传调用mutator() {
            Message expected = Message.builder().role("user").content("from context").build();
            setCallback(new AgentExecutionContext.AgentContextMutator.SendUserMessageCallback() {
                @Override
                public Message send(Long childSessionId, String content, Long modelId) {
                    return expected;
                }
            });

            Message result = context.sendUserMessage(2L, "from context", 200L);
            assertNotNull(result);
            assertEquals("user", result.getRole());
            assertEquals("from context", result.getContent());
        }

        @Test
        void 反向_context_sendUserMessage_mutator回调为null返回null() {
            assertNull(context.sendUserMessage(2L, "any", 200L));
        }

        @Test
        void 正向_context_sendUserMessage_透传参数给mutator() {
            AtomicReference<Long> capturedSessionId = new AtomicReference<>();
            AtomicReference<String> capturedContent = new AtomicReference<>();
            AtomicReference<Long> capturedModelId = new AtomicReference<>();
            setCallback(new AgentExecutionContext.AgentContextMutator.SendUserMessageCallback() {
                @Override
                public Message send(Long childSessionId, String content, Long modelId) {
                    capturedSessionId.set(childSessionId);
                    capturedContent.set(content);
                    capturedModelId.set(modelId);
                    return Message.builder().role("user").content(content).build();
                }
            });

            context.sendUserMessage(99L, "paramTest", 300L);
            assertEquals(Long.valueOf(99L), capturedSessionId.get());
            assertEquals("paramTest", capturedContent.get());
            assertEquals(Long.valueOf(300L), capturedModelId.get());
        }
    }

    @Nested
    class ExecutionContextParentDelegation {

        @Test
        void 正向_根会话getSessionVariable返回本地值() {
            context.putSessionVariable("rootKey", "rootVal");
            assertEquals("rootVal", context.getSessionVariable("rootKey"));
        }

        @Test
        void 正向_根会话getConversationVariable返回本地值() {
            context.putConversationVariable("convRoot", "convRootVal");
            assertEquals("convRootVal", context.getConversationVariable("convRoot"));
        }

        @Test
        void 正向_根会话getSessionVariableKeys返回本地keySet() {
            context.putSessionVariable("a", "1");
            context.putSessionVariable("b", "2");
            assertTrue(context.getSessionVariableKeys().containsAll(Set.of("a", "b")));
        }

        @Test
        void 正向_根会话getConversationVariableKeys返回本地keySet() {
            context.putConversationVariable("ca", "1");
            context.putConversationVariable("cb", "2");
            assertTrue(context.getConversationVariableKeys().containsAll(Set.of("ca", "cb")));
        }

        @Test
        void 正向_子会话getSessionVariable委托mutator回调() {
            var childMutator = new AgentExecutionContext.AgentContextMutator();
            var parentVars = new HashMap<String, String>();
            parentVars.put("parentKey", "parentVal");

            AgentExecutionContext parentCtx = new AgentExecutionContext(
                    1L, 1L, "p", 1L, 10, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new AgentExecutionContext.AgentContextMutator(), parentVars, new HashMap<>(), null, null);

            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getSessionVarCallback");
                f.setAccessible(true);
                f.set(childMutator, (Function<String, String>) parentCtx::getSessionVariable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            AgentExecutionContext childCtx = new AgentExecutionContext(
                    2L, 1L, "c", 1L, 10, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    childMutator, new HashMap<>(), new HashMap<>(), 1L, null);

            assertEquals("parentVal", childCtx.getSessionVariable("parentKey"));
        }

        @Test
        void 正向_子会话getSessionVariableKeys委托mutator回调() {
            var childMutator = new AgentExecutionContext.AgentContextMutator();
            var parentVars = new HashMap<String, String>();
            parentVars.put("pk1", "pv1");
            parentVars.put("pk2", "pv2");

            AgentExecutionContext parentCtx = new AgentExecutionContext(
                    1L, 1L, "p", 1L, 10, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new AgentExecutionContext.AgentContextMutator(), parentVars, new HashMap<>(), null, null);

            try {
                Field f = AgentExecutionContext.AgentContextMutator.class.getDeclaredField("getSessionVarKeysCallback");
                f.setAccessible(true);
                f.set(childMutator, (Supplier<Set<String>>) parentCtx::getSessionVariableKeys);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            AgentExecutionContext childCtx = new AgentExecutionContext(
                    2L, 1L, "c", 1L, 10, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    childMutator, new HashMap<>(), new HashMap<>(), 1L, null);

            Set<String> keys = childCtx.getSessionVariableKeys();
            assertTrue(keys.containsAll(Set.of("pk1", "pk2")));
        }
    }
}
