package com.ghost616.agentbase.sendmessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationIdMessageTest {

    @Test
    void MessageName_CONVERSATION_ID常量值为字符串CONVERSATION_ID() {
        assertEquals("CONVERSATION_ID", MessageName.CONVERSATION_ID);
    }

    @Test
    void 构造时sessionId写入父类字段且conversationId可读取() {
        ConversationIdMessage message = new ConversationIdMessage("s1", "conv-1");
        assertEquals("s1", message.getSessionId());
        assertEquals("conv-1", message.getConversationId());
    }

    @Test
    void getMessageName返回MessageName_CONVERSATION_ID() {
        ConversationIdMessage message = new ConversationIdMessage("s1", "conv-1");
        assertEquals(MessageName.CONVERSATION_ID, message.getMessageName());
    }

    @Test
    void 空字符串conversationId也能正常存取() {
        ConversationIdMessage message = new ConversationIdMessage("s2", "");
        assertEquals("s2", message.getSessionId());
        assertEquals("", message.getConversationId());
    }

    @Test
    void null的conversationId也能正常存取() {
        ConversationIdMessage message = new ConversationIdMessage("s3", null);
        assertEquals("s3", message.getSessionId());
        assertEquals(null, message.getConversationId());
    }
}
