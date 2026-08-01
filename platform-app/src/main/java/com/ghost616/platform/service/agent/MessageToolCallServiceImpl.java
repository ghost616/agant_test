package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghost616.platform.entity.MessageToolCall;
import com.ghost616.platform.repository.MessageToolCallMapper;
import org.springframework.stereotype.Service;

@Service
public class MessageToolCallServiceImpl extends ServiceImpl<MessageToolCallMapper, MessageToolCall> implements MessageToolCallService {
}
