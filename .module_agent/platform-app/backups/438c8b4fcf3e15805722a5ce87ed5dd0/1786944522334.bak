package com.ghost616.platform.service.session;

import com.ghost616.platform.dto.session.SessionDTO;

import java.util.List;

import com.ghost616.agentbase.service.agent.MessageDataProvider;


public interface SessionService {

    List<SessionDTO> listSessions(Long agentId);

    /**
     * 返回当前用户的所有主会话（isChild 为 null 或 false），
     * 不过滤 isEvaluation（含评估会话），按创建时间倒序。
     * 用于会话日志页等需要展示全部主会话的场景。
     *
     * @return 主会话 DTO 列表
     */
    List<SessionDTO> listLogSessions();

    SessionDTO createSession(Long agentId, Long modelId, String title);

    SessionDTO getSession(Long id);

    void deleteSession(Long id);

    List<MessageDataProvider.MessageDTO> getMessages(Long sessionId);

    List<MessageDataProvider.MessageDTO> getMessagesByConversationId(String conversationId);

    int rollback(Long sessionId);

    List<SessionDTO> listChildSessions(Long parentId);

    void updateThinking(Long sessionId, Boolean thinking);
}
