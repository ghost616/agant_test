package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.AggregationType;
import com.ghost616.platform.model.SessionMemoryDocument;
import com.ghost616.platform.service.agent.DefaultMessageDataProvider;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;
import com.ghost616.platform.service.memory.SessionMemoryService;
import com.ghost616.platform.service.message.MessageService;
import com.ghost616.platform.service.search.SessionMemoryESClient;
import com.ghost616.platform.service.session.SessionService;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接口级补充测试：GET /api/sessions/{id}/memory 的 userId 过滤参数。
 *
 * <p>验证 SessionController.queryMemory 在已登录时把当前登录用户 ID 透传给
 * {@link SessionMemoryESClient#queryBySessionId}（非空时 ES 侧追加 userId term 过滤），
 * 未登录时传 null（不追加用户过滤）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SessionControllerMemoryUserIdSupplementTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private DefaultSubSessionCallback subSessionCallback;

    @Mock
    private SessionMemoryService sessionMemoryService;

    @Mock
    private SessionMemoryESClient sessionMemoryESClient;

    @Mock
    private MessageService messageService;

    @Mock
    private DefaultMessageDataProvider defaultMessageDataProvider;

    @InjectMocks
    private SessionController controller;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void login(Long userId) {
        User user = new User();
        user.setId(userId);
        UserContext.set(new UserSession("mem-ctx-session", user, System.currentTimeMillis()));
    }

    @Test
    @DisplayName("GET /{id}/memory：已登录时把当前用户 userId 传入 ES 查询（接口用户隔离）")
    void queryMemory_已登录_透传当前用户userId() {
        login(42L);
        PageResult<SessionMemoryDocument> pageResult = new PageResult<>(List.of(), 0L, 1, 20);
        when(sessionMemoryESClient.queryBySessionId("100", 42L, AggregationType.GROUP, 1, 20))
                .thenReturn(pageResult);

        ApiResponse<PageResult<SessionMemoryDocument>> response =
                controller.queryMemory(100L, AggregationType.GROUP, 1, 20);

        assertTrue(response.isSuccess());
        assertSame(pageResult, response.getData());
        verify(sessionMemoryESClient).queryBySessionId("100", 42L, AggregationType.GROUP, 1, 20);
    }

    @Test
    @DisplayName("GET /{id}/memory：未登录时 userId 传 null，不追加用户过滤")
    void queryMemory_未登录_userId为null() {
        UserContext.clear();
        PageResult<SessionMemoryDocument> pageResult = new PageResult<>(List.of(), 0L, 1, 20);
        when(sessionMemoryESClient.queryBySessionId("100", null, AggregationType.DAILY, 1, 20))
                .thenReturn(pageResult);

        ApiResponse<PageResult<SessionMemoryDocument>> response =
                controller.queryMemory(100L, AggregationType.DAILY, 1, 20);

        assertTrue(response.isSuccess());
        verify(sessionMemoryESClient).queryBySessionId("100", null, AggregationType.DAILY, 1, 20);
    }
}