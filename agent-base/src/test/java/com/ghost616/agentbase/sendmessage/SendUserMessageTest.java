package com.ghost616.agentbase.sendmessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendUserMessageTest {

    @Test
    void MessageName_SEND_USER_MESSAGE常量值为字符串SEND_USER_MESSAGE() {
        assertEquals("SEND_USER_MESSAGE", MessageName.SEND_USER_MESSAGE);
    }

    @Test
    void 构造时写入sessionId与content及会话字段() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", "conv-1", "parent-1", "main-1");
        assertEquals("child-1", message.getSessionId());
        assertEquals("hello", message.getContent());
        assertEquals("conv-1", message.getConversationId());
        assertEquals("parent-1", message.getParentSessionId());
        assertEquals("main-1", message.getMainSessionId());
    }

    @Test
    void getMessageName返回MessageName_SEND_USER_MESSAGE() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", null, null, null);
        assertEquals(MessageName.SEND_USER_MESSAGE, message.getMessageName());
    }

    @Test
    void null的会话字段也能正常存取() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", null, null, null);
        assertEquals("child-1", message.getSessionId());
        assertEquals("hello", message.getContent());
        assertEquals(null, message.getConversationId());
        assertEquals(null, message.getParentSessionId());
        assertEquals(null, message.getMainSessionId());
    }

    @Test
    void 继承自SessionMessage的getter_setter可正常使用() {
        SendUserMessage message = new SendUserMessage("child-1", "hello", null, null, null);
        message.setConversationId("conv-2");
        message.setParentSessionId("parent-2");
        message.setMainSessionId("main-2");
        assertEquals("conv-2", message.getConversationId());
        assertEquals("parent-2", message.getParentSessionId());
        assertEquals("main-2", message.getMainSessionId());
    }
}
