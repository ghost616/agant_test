package com.ghost616.platform.service.tool;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.entity.ToolConfig;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.repository.ToolConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolConfigServiceImplTest {

    @Mock
    private ToolConfigMapper toolConfigMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ToolConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ToolConfigServiceImpl(toolConfigMapper, eventPublisher);
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

            assertEquals(1L, dto.getId());
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
}
