package com.ghost616.platform.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.session.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息查询服务实现，通过 MessageMapper 查询消息持久化数据。
 *
 * <p>查询仅返回当前登录用户（{@link UserContextUtil}）的数据，实现消息数据用户隔离。</p>
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public List<Message> getAllMessages(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, UserContextUtil.requireUserId())
                .eq(Message::getSessionId, sessionId)
                .eq(Message::getRollback, false)
                .orderByAsc(Message::getSequenceNum);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public List<Message> getMessagesBySeqRange(Long sessionId, Integer startSeq, Integer endSeq) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, UserContextUtil.requireUserId())
                .eq(Message::getSessionId, sessionId)
                .eq(Message::getRollback, false)
                .ge(Message::getSequenceNum, startSeq)
                .le(Message::getSequenceNum, endSeq)
                .orderByAsc(Message::getSequenceNum);
        return messageMapper.selectList(wrapper);
    }
}
