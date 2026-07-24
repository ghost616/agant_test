package com.ghost616.platform.dto.tool;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.platform.enums.SubToolType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ToolDetailDTOTest {

    @Test
    void dto_shouldExtendToolConfigDTO() {
        assertTrue(ToolDetailDTO.class.getSuperclass().equals(ToolConfigDTO.class));
    }

    @Test
    void dto_shouldContainToolScriptAndSubToolTypeFields() throws Exception {
        assertNotNull(ToolDetailDTO.class.getDeclaredField("toolScript"));
        assertNotNull(ToolDetailDTO.class.getDeclaredField("subToolType"));
    }

    @Test
    void builder_shouldSetAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ToolDetailDTO dto = ToolDetailDTO.builder()
                .id(1L)
                .name("test_tool")
                .toolType(ToolType.CUSTOM)
                .description("desc")
                .parameterSchema("{}")
                .returnSchema("{}")
                .implPath("/path/to/tool")
                .authConfig("{}")
                .subToolType(SubToolType.BROWSER)
                .toolScript("console.log('hello')")
                .status(CommonStatus.ENABLED)
                .createTime(now)
                .updateTime(now)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("test_tool", dto.getName());
        assertEquals(ToolType.CUSTOM, dto.getToolType());
        assertEquals("desc", dto.getDescription());
        assertEquals("{}", dto.getParameterSchema());
        assertEquals("{}", dto.getReturnSchema());
        assertEquals("/path/to/tool", dto.getImplPath());
        assertEquals("{}", dto.getAuthConfig());
        assertEquals(SubToolType.BROWSER, dto.getSubToolType());
        assertEquals("console.log('hello')", dto.getToolScript());
        assertEquals(CommonStatus.ENABLED, dto.getStatus());
        assertEquals(now, dto.getCreateTime());
        assertEquals(now, dto.getUpdateTime());
    }
}
