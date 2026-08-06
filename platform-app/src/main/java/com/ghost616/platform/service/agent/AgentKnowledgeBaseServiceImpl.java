package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghost616.platform.entity.AgentKnowledgeBase;
import com.ghost616.platform.repository.AgentKnowledgeBaseMapper;
import org.springframework.stereotype.Service;

@Service
public class AgentKnowledgeBaseServiceImpl extends ServiceImpl<AgentKnowledgeBaseMapper, AgentKnowledgeBase> implements AgentKnowledgeBaseService {
}
