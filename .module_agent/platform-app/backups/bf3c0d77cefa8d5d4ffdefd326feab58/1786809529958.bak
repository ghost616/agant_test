package com.ghost616.platform.service.model;

import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 补充测试：ModelConfigServiceImpl 未登录保护（数据用户隔离）。
 *
 * <p>当 UserContext 为空或会话用户为 null 时，create/list 必须抛出
 * {@link ErrorCode#USER_NOT_LOGIN}，防止无归属数据写入与越权查询。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplAuthGuardTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private ModelInvokerManager modelInvokerManager;

    private ModelConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModelConfigServiceImpl(modelConfigMapper, modelInvokerManager);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ModelCreateRequest createRequest() {
        return ModelCreateRequest.builder()
                .name("test-model")
                .platformType(PlatformType.OPENAI)
                .apiKey("sk-test")
                .build();
    }

    @Test
    @DisplayName("UserContext 为空时 create() 抛出 USER_NOT_LOGIN 且不执行插入")
    void create_UserContext为空抛USER_NOT_LOGIN() {
        UserContext.clear();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(createRequest()));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(modelConfigMapper, never()).insert(any());
    }

    @Test
    @DisplayName("UserContext 为空时 list() 抛出 USER_NOT_LOGIN 且不执行查询")
    void list_UserContext为空抛USER_NOT_LOGIN() {
        UserContext.clear();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(null, null, null, null));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(modelConfigMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("会话用户为 null 时 create() 抛出 USER_NOT_LOGIN 且不执行插入")
    void create_用户为null抛USER_NOT_LOGIN() {
        UserContext.set(new UserSession("session-null-user", null, System.currentTimeMillis()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(createRequest()));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(modelConfigMapper, never()).insert(any());
    }

    @Test
    @DisplayName("会话用户为 null 时 list() 抛出 USER_NOT_LOGIN 且不执行查询")
    void list_用户为null抛USER_NOT_LOGIN() {
        UserContext.set(new UserSession("session-null-user", null, System.currentTimeMillis()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(null, null, null, null));

        assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
        verify(modelConfigMapper, never()).selectList(any());
    }
}