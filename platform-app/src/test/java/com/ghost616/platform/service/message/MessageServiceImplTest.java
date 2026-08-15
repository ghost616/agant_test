package com.ghost616.platform.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    private static final Long CURRENT_USER_ID = 42L;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message message1;
    private Message message2;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserSession session = new UserSession("session-1", user, System.currentTimeMillis());
        UserContext.set(session);

        message1 = new Message();
        message1.setId(1L);
        message1.setUserId(CURRENT_USER_ID);
        message1.setSessionId(100L);
        message1.setRole("user");
        message1.setContent("hello");
        message1.setSequenceNum(1);
        message1.setRollback(false);

        message2 = new Message();
        message2.setId(2L);
        message2.setUserId(CURRENT_USER_ID);
        message2.setSessionId(100L);
        message2.setRole("assistant");
        message2.setContent("hi");
        message2.setSequenceNum(2);
        message2.setRollback(false);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getAllMessages_有消息_返回消息列表() {
        when(messageMapper.selectList(any())).thenReturn(List.of(message1, message2));

        List<Message> result = messageService.getAllMessages(100L);

        assertEquals(2, result.size());
        assertEquals("hello", result.get(0).getContent());
        assertEquals("hi", result.get(1).getContent());
        verify(messageMapper).selectList(any());
    }

    @Test
    void getAllMessages_无消息_返回空列表() {
        when(messageMapper.selectList(any())).thenReturn(List.of());

        List<Message> result = messageService.getAllMessages(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllMessages_查询条件包含sessionId和rollback为false() {
        when(messageMapper.selectList(any())).thenReturn(List.of());

        messageService.getAllMessages(100L);

        ArgumentCaptor<LambdaQueryWrapper<Message>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(messageMapper).selectList(captor.capture());
        LambdaQueryWrapper<Message> wrapper = captor.getValue();
        assertNotNull(wrapper);
    }

    @Test
    void getAllMessages_不做memoryPoint过滤() {
        when(messageMapper.selectList(any())).thenReturn(List.of(message1));

        messageService.getAllMessages(100L);

        verify(messageMapper).selectList(any());
        verifyNoMoreInteractions(messageMapper);
    }

    @Test
    void getMessagesBySeqRange_有消息_返回区间内消息列表() {
        when(messageMapper.selectList(any())).thenReturn(List.of(message1, message2));

        List<Message> result = messageService.getMessagesBySeqRange(100L, 1, 5);

        assertEquals(2, result.size());
        assertEquals("hello", result.get(0).getContent());
        assertEquals("hi", result.get(1).getContent());
        verify(messageMapper).selectList(any());
    }

    @Test
    void getMessagesBySeqRange_无消息_返回空列表() {
        when(messageMapper.selectList(any())).thenReturn(List.of());

        List<Message> result = messageService.getMessagesBySeqRange(100L, 10, 20);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMessagesBySeqRange_查询条件包含sessionId_rollback和sequenceNum区间() {
        when(messageMapper.selectList(any())).thenReturn(List.of());

        messageService.getMessagesBySeqRange(100L, 3, 8);

        ArgumentCaptor<LambdaQueryWrapper<Message>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(messageMapper).selectList(captor.capture());
        LambdaQueryWrapper<Message> wrapper = captor.getValue();
        assertNotNull(wrapper);
    }
}
