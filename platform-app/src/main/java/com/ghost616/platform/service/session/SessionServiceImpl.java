package com.ghost616.platform.service.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.dto.session.SessionDTO;
import com.ghost616.platform.entity.AgentTool;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;


@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final AgentToolMapper agentToolMapper;
    private final SessionToolMapper sessionToolMapper;
    private final SessionManager sessionManager;
    private final AgentContextManager agentContextManager;
    private final ToolManager toolManager;

    @Override
    public List<SessionDTO> listSessions(Long agentId) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        if (agentId != null) {
            wrapper.eq(Session::getAgentId, agentId);
        }
        wrapper.and(w -> w.isNull(Session::getIsChild).or().eq(Session::getIsChild, false));
        wrapper.orderByDesc(Session::getCreateTime);

        List<Session> entities = sessionMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public SessionDTO createSession(Long agentId, Long modelId, String title) {
        Session entity = new Session();
        entity.setAgentId(agentId);
        entity.setModelId(modelId);
        entity.setTitle(title);
        entity.setIsChild(false);
        sessionMapper.insert(entity);

        LambdaQueryWrapper<AgentTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(AgentTool::getAgentId, agentId);
        List<AgentTool> agentTools = agentToolMapper.selectList(toolWrapper);
        if (!agentTools.isEmpty()) {
            Long sessionId = entity.getId();
            for (AgentTool agentTool : agentTools) {
                SessionTool sessionTool = new SessionTool();
                sessionTool.setSessionId(sessionId);
                sessionTool.setToolId(agentTool.getToolId());
                sessionToolMapper.insert(sessionTool);
            }
        }

        return toDTO(entity);
    }

    @Override
    public SessionDTO getSession(Long id) {
        Session entity = sessionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void deleteSession(Long id) {
        Session entity = sessionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        LambdaQueryWrapper<SessionTool> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SessionTool::getSessionId, id);
        sessionToolMapper.delete(deleteWrapper);

        sessionMapper.deleteById(id);
        agentContextManager.remove(id);
        toolManager.clearSessionCache(id);
    }

    @Override
    @Transactional
    public int rollback(Long sessionId) {
        Session entity = sessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        int deleted = sessionManager.rollbackToLastUserMessage(sessionId);
        agentContextManager.remove(sessionId);
        return deleted;
    }

    @Override
    public List<MessageDataProvider.MessageDTO> getMessages(Long sessionId) {
        Session entity = sessionMapper.selectById(sessionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        return sessionManager.getMessages(sessionId);
    }

    private SessionDTO toDTO(Session entity) {
        return SessionDTO.builder()
                .id(entity.getId())
                .agentId(entity.getAgentId())
                .modelId(entity.getModelId())
                .title(entity.getTitle())
                .systemPrompt(entity.getSystemPrompt())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
