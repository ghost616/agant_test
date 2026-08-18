package com.ghost616.agentbase.sendmessage;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChildCreateSessionTest {

    private final AgentExecutionContext.ChildSession child =
            new AgentExecutionContext.ChildSession("10", "sub-agent", "desc", "300");

    @Test
    void 构造后parentSessionIds为仅含parentSessionId的单元素列表() {
        ChildCreateSession message = new ChildCreateSession("parent-1", child);

        assertEquals(List.of("parent-1"), message.getParentSessionIds());
        assertEquals("parent-1", message.getSessionId());
    }

    @Test
    void 构造后childSession可正常存取() {
        ChildCreateSession message = new ChildCreateSession("parent-1", child);

        assertEquals(child, message.getChildSession());
        assertEquals("10", message.getChildSession().sessionId());
        assertEquals("sub-agent", message.getChildSession().sessionName());
    }

    @Test
    void getMessageName返回CHILD_SESSION() {
        ChildCreateSession message = new ChildCreateSession("parent-1", child);

        assertEquals(MessageName.CHILD_SESSION, message.getMessageName());
    }

    @Test
    void 继承自SessionMessage的conversationId与parentSessionIds可写() {
        ChildCreateSession message = new ChildCreateSession("parent-1", child);

        assertNull(message.getConversationId());
        message.setConversationId("conv-1");
        message.setParentSessionIds(List.of("level2", "level1", "main"));

        assertEquals("conv-1", message.getConversationId());
        assertEquals(List.of("level2", "level1", "main"), message.getParentSessionIds());
    }
}