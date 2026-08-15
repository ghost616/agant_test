package com.ghost616.platform.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.entity.Message;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.MessageMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息查询服务实现，通过 MessageMapper 查询消息持久化数据。
 *
 * <p>查询仅返回当前登录用户（{@link UserContext}）的数据，实现消息数据用户隔离。</p>
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public List<Message> getAllMessages(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, currentUserId())
                .eq(Message::getSessionId, sessionId)
                .eq(Message::getRollback, false)
                .orderByAsc(Message::getSequenceNum);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public List<Message> getMessagesBySeqRange(Long sessionId, Integer startSeq, Integer endSeq) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, currentUserId())
                .eq(Message::getSessionId, sessionId)
                .eq(Message::getRollback, false)
                .ge(Message::getSequenceNum, startSeq)
                .le(Message::getSequenceNum, endSeq)
                .orderByAsc(Message::getSequenceNum);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * <p>从 {@link UserContext} 线程上下文读取用户会话；
     * 未登录时抛出 {@link ErrorCode#USER_NOT_LOGIN}，防止越权查询其他用户消息。</p>
     *
     * @return 当前登录用户 ID
     */
    private Long currentUserId() {
        UserSession session = UserContext.get();
        if (session == null || session.getUser() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return session.getUser().getId();
    }
}
