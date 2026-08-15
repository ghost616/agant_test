package com.ghost616.platform.service.tool;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentinteg.tool.KnowledgeBaseInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeFileChunkTool;
import com.ghost616.agentinteg.tool.KnowledgeFileInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeSearchTool;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.entity.ToolConfig;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.repository.ToolConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolConfigServiceImplTest {

    @Mock
    private ToolConfigMapper toolConfigMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ToolManager toolManager;

    /** 测试用当前登录用户 ID。 */
    private static final Long CURRENT_USER_ID = 100L;

    private ToolConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis-Plus TableInfo 缓存，使 LambdaQueryWrapper.getSqlSegment() 在纯单测环境可解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ToolConfig.class);
        service = new ToolConfigServiceImpl(toolConfigMapper, eventPublisher, toolManager);
        User user = new User();
        user.setId(CURRENT_USER_ID);
        UserContext.set(new UserSession("test-session", user, System.currentTimeMillis()));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ToolConfig createEntity(Long id, String name) {
        ToolConfig entity = new ToolConfig();
        entity.setId(id);
        entity.setName(name);
        entity.setToolType(ToolType.CUSTOM);
        entity.setDescription("desc");
        entity.setParameterSchema("{}");
        entity.setReturnSchema("{}");
        entity.setImplPath("/path/to/tool");
        entity.setAuthConfig("{}");
        entity.setStatus(CommonStatus.ENABLED);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    @Nested
    class CreateTests {

        @Test
        void browserSubType_withNullToolType_shouldSetToCUSTOM() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);
            doAnswer(inv -> {
                ToolConfig arg = inv.getArgument(0);
                arg.setId(1L);
                arg.setCreateTime(LocalDateTime.now());
                arg.setUpdateTime(LocalDateTime.now());
                return null;
            }).when(toolConfigMapper).insert(any(ToolConfig.class));

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("browser_tool")
                    .toolType(null)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("console.log('test')")
                    .build();

            ToolDetailDTO result = service.create(request);

            assertEquals(ToolType.CUSTOM, result.getToolType());
            assertEquals(SubToolType.BROWSER, result.getSubToolType());
            assertEquals("console.log('test')", result.getToolScript());
        }

        @Test
        void browserSubType_withNullToolScript_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("browser_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript(null)
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withBlankToolScript_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("browser_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("   ")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
        }

        @Test
        void nonBrowserSubType_withNullImplPath_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("normal_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(null)
                    .implPath(null)
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("实现路径"));
        }

        @Test
        void browserSubType_withOnlyComments_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("comment_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("// just a comment")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withOnlyBlockComments_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("block_comment_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("/* block comment */")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withMismatchedBrackets_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("bracket_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("function() { console.log('test') ")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("语法错误"));
        }

        @Test
        void browserSubType_withCrossMismatchedBrackets_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("cross_bracket_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("({[)}")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
        }

        @Test
        void browserSubType_withExtraClosingBracket_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("extra_close_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("function() }")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
        }

        @Test
        void browserSubType_withBracketsInDoubleQuotes_shouldSucceed() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);
            doAnswer(inv -> {
                ToolConfig arg = inv.getArgument(0);
                arg.setId(1L);
                return null;
            }).when(toolConfigMapper).insert(any(ToolConfig.class));

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("string_bracket_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("var s = \"test(\"")
                    .build();

            ToolDetailDTO result = service.create(request);
            assertEquals("var s = \"test(\"", result.getToolScript());
        }

        @Test
        void browserSubType_withBracketsInSingleQuotes_shouldSucceed() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);
            doAnswer(inv -> {
                ToolConfig arg = inv.getArgument(0);
                arg.setId(1L);
                return null;
            }).when(toolConfigMapper).insert(any(ToolConfig.class));

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("single_quote_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("var s = 'test('")
                    .build();

            ToolDetailDTO result = service.create(request);
            assertEquals("var s = 'test('", result.getToolScript());
        }

        @Test
        void browserSubType_withNestedValidBrackets_shouldSucceed() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);
            doAnswer(inv -> {
                ToolConfig arg = inv.getArgument(0);
                arg.setId(1L);
                return null;
            }).when(toolConfigMapper).insert(any(ToolConfig.class));

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("nested_bracket_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("({[]})")
                    .build();

            ToolDetailDTO result = service.create(request);
            assertEquals("({[]})", result.getToolScript());
        }

        @Test
        void create_shouldFillUserIdFromUserContext() {
            when(toolConfigMapper.selectCount(any())).thenReturn(0L);
            doAnswer(inv -> {
                ToolConfig arg = inv.getArgument(0);
                arg.setId(1L);
                arg.setCreateTime(LocalDateTime.now());
                arg.setUpdateTime(LocalDateTime.now());
                return null;
            }).when(toolConfigMapper).insert(any(ToolConfig.class));

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("user_tool")
                    .toolType(ToolType.CUSTOM)
                    .subToolType(null)
                    .implPath("/path/to/tool")
                    .build();

            service.create(request);

            ArgumentCaptor<ToolConfig> captor = ArgumentCaptor.forClass(ToolConfig.class);
            verify(toolConfigMapper).insert(captor.capture());
            assertEquals(CURRENT_USER_ID, captor.getValue().getUserId(),
                    "create 应把当前登录用户 ID 填充到 user_id 字段");
        }

        @Test
        void create_withoutUserContext_shouldThrowUserNotLogin() {
            UserContext.clear();

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("no_login_tool")
                    .toolType(ToolType.CUSTOM)
                    .implPath("/path/to/tool")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(toolConfigMapper, never()).insert(any(ToolConfig.class));
        }

        @Test
        void create_withDuplicateNameOfSameUser_shouldThrow() {
            when(toolConfigMapper.selectCount(any())).thenReturn(1L);

            ToolCreateRequest request = ToolCreateRequest.builder()
                    .name("dup_tool")
                    .toolType(ToolType.CUSTOM)
                    .implPath("/path/to/tool")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
            assertEquals(ErrorCode.TOOL_ALREADY_EXISTS, ex.getErrorCode());
        }
    }

    @Nested
    class ListTests {

        @Test
        @SuppressWarnings("unchecked")
        void list_shouldFilterByCurrentUserId() {
            when(toolConfigMapper.selectList(any())).thenReturn(List.of(createEntity(1L, "my_tool")));

            service.list(null, null, null);

            ArgumentCaptor<LambdaQueryWrapper<ToolConfig>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(toolConfigMapper).selectList(captor.capture());
            // MyBatis-Plus 参数惰性填充：调用 getSqlSegment() 渲染 SQL 后才写入参数
            captor.getValue().getSqlSegment();
            assertTrue(captor.getValue().getParamNameValuePairs().containsValue(CURRENT_USER_ID),
                    "list 应使用当前登录用户 ID 过滤 user_id");
        }

        @Test
        @SuppressWarnings("unchecked")
        void list_shouldUseUserFromCurrentContext() {
            when(toolConfigMapper.selectList(any())).thenReturn(List.of());

            UserContext.clear();
            User other = new User();
            other.setId(200L);
            UserContext.set(new UserSession("other-session", other, System.currentTimeMillis()));

            service.list(null, null, null);

            ArgumentCaptor<LambdaQueryWrapper<ToolConfig>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(toolConfigMapper).selectList(captor.capture());
            // MyBatis-Plus 参数惰性填充：调用 getSqlSegment() 渲染 SQL 后才写入参数
            captor.getValue().getSqlSegment();
            assertTrue(captor.getValue().getParamNameValuePairs().containsValue(200L),
                    "list 应使用当前线程上下文的用户 ID 过滤");
            assertFalse(captor.getValue().getParamNameValuePairs().containsValue(CURRENT_USER_ID),
                    "不应混入其他用户的 user_id 过滤值");
        }

        @Test
        void list_shouldReturnCurrentUserTools() {
            ToolConfig entity = createEntity(1L, "my_tool");
            when(toolConfigMapper.selectList(any())).thenReturn(List.of(entity));

            List<ToolDetailDTO> result = service.list(null, null, null);

            assertEquals(1, result.size());
            assertEquals("my_tool", result.get(0).getName());
        }

        @Test
        void list_withoutUserContext_shouldThrowUserNotLogin() {
            UserContext.clear();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.list(null, null, null));
            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(toolConfigMapper, never()).selectList(any());
        }
    }

    @Nested
    class GetImplByNameTests {

        @Test
        @SuppressWarnings("unchecked")
        void getImplByName_shouldFilterByCurrentUserId() {
            when(toolConfigMapper.selectOne(any())).thenReturn(createEntity(1L, "impl_tool"));

            service.getImplByName("impl_tool");

            ArgumentCaptor<LambdaQueryWrapper<ToolConfig>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(toolConfigMapper).selectOne(captor.capture());
            // MyBatis-Plus 参数惰性填充：调用 getSqlSegment() 渲染 SQL 后才写入参数
            captor.getValue().getSqlSegment();
            assertTrue(captor.getValue().getParamNameValuePairs().containsValue(CURRENT_USER_ID),
                    "getImplByName 应使用当前登录用户 ID 过滤 user_id");
            assertTrue(captor.getValue().getParamNameValuePairs().containsValue("impl_tool"),
                    "getImplByName 应按名称查询");
        }

        @Test
        void getImplByName_withoutUserContext_shouldThrowUserNotLogin() {
            UserContext.clear();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.getImplByName("impl_tool"));
            assertEquals(ErrorCode.USER_NOT_LOGIN, ex.getErrorCode());
            verify(toolConfigMapper, never()).selectOne(any());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void browserSubType_withBlankToolScript_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withNullToolScript_shouldKeepExisting() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setToolScript("existing script");
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript(null)
                    .build();

            ToolDetailDTO result = service.update(1L, request);

            assertEquals(SubToolType.BROWSER, result.getSubToolType());
            assertEquals("existing script", result.getToolScript());
        }

        @Test
        void browserSubType_withValidToolScript_shouldUpdate() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("new script")
                    .build();

            ToolDetailDTO result = service.update(1L, request);

            assertEquals(SubToolType.BROWSER, result.getSubToolType());
            assertEquals("new script", result.getToolScript());
        }

        @Test
        void nonBrowserSubType_withNullImplPath_shouldKeepExisting() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setImplPath("original/path");
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(null)
                    .implPath(null)
                    .build();

            ToolDetailDTO result = service.update(1L, request);

            assertEquals("original/path", result.getImplPath());
        }

        @Test
        void nonBrowserSubType_withImplPath_shouldCallValidateImplPath() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("new script")
                    .build();

            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolDetailDTO result = service.update(1L, request);
            assertNotNull(result);
        }

        @Test
        void browserSubType_withOnlyComments_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("// comment only")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withOnlyBlockComments_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("/* block comment */")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("toolScript"));
        }

        @Test
        void browserSubType_withMismatchedBrackets_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("if (true { return; }")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("语法错误"));
        }

        @Test
        void browserSubType_withCrossMismatchedBrackets_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("({[)}")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
        }

        @Test
        void browserSubType_withExtraClosingBracket_shouldThrow() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("function() }")
                    .build();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, request));
            assertEquals(ErrorCode.TOOL_SCHEMA_INVALID, ex.getErrorCode());
        }

        @Test
        void browserSubType_withBracketsInDoubleQuotes_shouldSucceed() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("var s = \"test(\"")
                    .build();

            ToolDetailDTO result = service.update(1L, request);
            assertEquals("var s = \"test(\"", result.getToolScript());
        }

        @Test
        void browserSubType_withBracketsInSingleQuotes_shouldSucceed() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("var s = 'test('")
                    .build();

            ToolDetailDTO result = service.update(1L, request);
            assertEquals("var s = 'test('", result.getToolScript());
        }

        @Test
        void browserSubType_withNestedValidBrackets_shouldSucceed() {
            ToolConfig existing = createEntity(1L, "existing_tool");
            existing.setSubToolType(null);
            existing.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(existing);
            when(toolConfigMapper.updateById(any(ToolConfig.class))).thenReturn(1);

            ToolUpdateRequest request = ToolUpdateRequest.builder()
                    .subToolType(SubToolType.BROWSER)
                    .toolScript("({[]})")
                    .build();

            ToolDetailDTO result = service.update(1L, request);
            assertEquals("({[]})", result.getToolScript());
        }
    }

    @Nested
    class ToDTOTests {

        @Test
        void toDTO_shouldMapAllFields() {
            ToolConfig entity = createEntity(1L, "test_tool");
            entity.setSubToolType(SubToolType.BROWSER);
            entity.setToolScript("console.log('test')");
            when(toolConfigMapper.selectById(1L)).thenReturn(entity);

            ToolDetailDTO dto = service.getById(1L);

            assertEquals("1", dto.getId());
            assertEquals("test_tool", dto.getName());
            assertEquals(ToolType.CUSTOM, dto.getToolType());
            assertEquals("desc", dto.getDescription());
            assertEquals("{}", dto.getParameterSchema());
            assertEquals("{}", dto.getReturnSchema());
            assertEquals("/path/to/tool", dto.getImplPath());
            assertEquals("{}", dto.getAuthConfig());
            assertEquals(SubToolType.BROWSER, dto.getSubToolType());
            assertEquals("console.log('test')", dto.getToolScript());
            assertEquals(CommonStatus.ENABLED, dto.getStatus());
            assertNotNull(dto.getCreateTime());
            assertNotNull(dto.getUpdateTime());
        }

        @Test
        void toDTO_shouldMapNullSubToolTypeAndToolScript() {
            ToolConfig entity = createEntity(1L, "test_tool");
            entity.setSubToolType(null);
            entity.setToolScript(null);
            when(toolConfigMapper.selectById(1L)).thenReturn(entity);

            ToolDetailDTO dto = service.getById(1L);

            assertNull(dto.getSubToolType());
            assertNull(dto.getToolScript());
        }
    }

    @Nested
    class ExceptionTests {

        @Test
        void getById_notFound_shouldThrow() {
            when(toolConfigMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(999L));
            assertEquals(ErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void delete_notFound_shouldThrow() {
            when(toolConfigMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(999L));
            assertEquals(ErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    class GetToolConfigBySessionAndNameTests {

        @Test
        void whenDtoNotFound_shouldReturnNull() {
            when(toolManager.getToolConfig("1", "unknown")).thenReturn(null);

            ToolConfig result = service.getToolConfigBySessionAndName(1L, "unknown");

            assertNull(result);
        }

        @Test
        void whenDtoFoundButEntityNotFound_shouldThrow() {
            ToolConfigDTO dto = ToolConfigDTO.builder().id("100").build();
            when(toolManager.getToolConfig("1", "ghost")).thenReturn(dto);
            when(toolConfigMapper.selectById(100L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getToolConfigBySessionAndName(1L, "ghost"));
            assertEquals(ErrorCode.TOOL_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void whenDtoIdIsNull_shouldReturnNull() {
            ToolConfigDTO dto = ToolConfigDTO.builder().id(null).build();
            when(toolManager.getToolConfig("1", "null-id")).thenReturn(dto);

            ToolConfig result = service.getToolConfigBySessionAndName(1L, "null-id");

            assertNull(result);
        }

        @Test
        void whenFound_shouldReturnEntity() {
            ToolConfig entity = new ToolConfig();
            entity.setId(100L);
            entity.setName("my_tool");
            ToolConfigDTO dto = ToolConfigDTO.builder().id("100").build();
            when(toolManager.getToolConfig("1", "my_tool")).thenReturn(dto);
            when(toolConfigMapper.selectById(100L)).thenReturn(entity);

            ToolConfig result = service.getToolConfigBySessionAndName(1L, "my_tool");

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("my_tool", result.getName());
        }

        @Test
        void whenDtoIdIsKnowledgeToolName_shouldReturnNull() {
            ToolConfigDTO dto = ToolConfigDTO.builder().id(KnowledgeBaseInfoTool.TOOL_NAME).build();
            when(toolManager.getToolConfig("1", KnowledgeBaseInfoTool.TOOL_NAME)).thenReturn(dto);

            ToolConfig result = service.getToolConfigBySessionAndName(1L, KnowledgeBaseInfoTool.TOOL_NAME);

            assertNull(result);
            verify(toolConfigMapper, never()).selectById(any());
        }
    }
}
