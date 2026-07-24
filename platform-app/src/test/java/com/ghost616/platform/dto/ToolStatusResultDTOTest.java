package com.ghost616.platform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolStatusResultDTOTest {

    private ToolStatusResultDTO.ToolConfigBrief createBrief(String id, String subToolType, String toolName) {
        ToolStatusResultDTO.ToolConfigBrief brief = new ToolStatusResultDTO.ToolConfigBrief();
        brief.setId(id);
        brief.setSubToolType(subToolType);
        brief.setToolName(toolName);
        return brief;
    }

    @Test
    void 无参构造器创建空对象() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO();
        assertNull(dto.getStatus());
        assertNull(dto.getToolId());
        assertNull(dto.getToolName());
        assertNull(dto.getArguments());
        assertFalse(dto.isHasMore());
        assertNull(dto.getResult());
        assertNull(dto.getMessage());
        assertFalse(dto.isNeedsSubSessionFlow());
        assertNull(dto.getToolConfig());
    }

    @Test
    void 全参构造器正确设置所有字段() {
        ToolStatusResultDTO.ToolConfigBrief brief = createBrief("42", "BROWSER", "my_tool");
        ToolStatusResultDTO dto = new ToolStatusResultDTO(
                "done", "tool-001", "file_reader", "--path=test.txt", true, "success result", "ok", true, brief);

        assertEquals("done", dto.getStatus());
        assertEquals("tool-001", dto.getToolId());
        assertEquals("file_reader", dto.getToolName());
        assertEquals("--path=test.txt", dto.getArguments());
        assertTrue(dto.isHasMore());
        assertEquals("success result", dto.getResult());
        assertEquals("ok", dto.getMessage());
        assertTrue(dto.isNeedsSubSessionFlow());
        assertSame(brief, dto.getToolConfig());
        assertEquals("42", brief.getId());
        assertEquals("BROWSER", brief.getSubToolType());
        assertEquals("my_tool", brief.getToolName());
    }

    @Test
    void 八参构造器toolConfig默认为null() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO(
                "done", "tool-001", "file_reader", "--path=test.txt", true, "success result", "ok", true);
        assertNull(dto.getToolConfig());
    }

    @Test
    void setter方法正确更新字段值() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO();
        dto.setStatus("running");
        dto.setToolId("tc-99");
        dto.setToolName("code_runner");
        dto.setArguments("{\"lang\":\"java\"}");
        dto.setHasMore(true);
        dto.setResult("output");
        dto.setMessage("completed");
        dto.setNeedsSubSessionFlow(true);
        ToolStatusResultDTO.ToolConfigBrief brief = createBrief("1", "BROWSER", "code_runner");
        dto.setToolConfig(brief);

        assertEquals("running", dto.getStatus());
        assertEquals("tc-99", dto.getToolId());
        assertEquals("code_runner", dto.getToolName());
        assertEquals("{\"lang\":\"java\"}", dto.getArguments());
        assertTrue(dto.isHasMore());
        assertEquals("output", dto.getResult());
        assertEquals("completed", dto.getMessage());
        assertTrue(dto.isNeedsSubSessionFlow());
        assertSame(brief, dto.getToolConfig());
    }

    @Test
    void 字段默认值验证() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO();
        assertFalse(dto.isHasMore(), "hasMore 默认应为 false");
        assertFalse(dto.isNeedsSubSessionFlow(), "needsSubSessionFlow 默认应为 false");
        assertNull(dto.getStatus());
        assertNull(dto.getToolId());
        assertNull(dto.getToolName());
        assertNull(dto.getArguments());
        assertNull(dto.getResult());
        assertNull(dto.getMessage());
        assertNull(dto.getToolConfig());
    }

    @Test
    void 全参构造器接收空值() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO(null, null, null, null, false, null, null, false, null);
        assertNull(dto.getStatus());
        assertNull(dto.getToolId());
        assertNull(dto.getToolName());
        assertNull(dto.getArguments());
        assertFalse(dto.isHasMore());
        assertNull(dto.getResult());
        assertNull(dto.getMessage());
        assertFalse(dto.isNeedsSubSessionFlow());
        assertNull(dto.getToolConfig());
    }

    @Test
    void equals和hashCode一致性() {
        ToolStatusResultDTO.ToolConfigBrief brief = createBrief("1", "BROWSER", "tool-a");
        ToolStatusResultDTO dto1 = new ToolStatusResultDTO(
                "idle", "t-1", "tool-a", "", false, null, null, false, brief);
        ToolStatusResultDTO dto2 = new ToolStatusResultDTO(
                "idle", "t-1", "tool-a", "", false, null, null, false, brief);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void equals不同对象不等() {
        ToolStatusResultDTO.ToolConfigBrief brief1 = createBrief("1", "BROWSER", "tool-a");
        ToolStatusResultDTO.ToolConfigBrief brief2 = createBrief("2", null, "tool-b");
        ToolStatusResultDTO dto1 = new ToolStatusResultDTO(
                "done", "t-1", "tool-a", "{}", true, "ok", "msg", true, brief1);
        ToolStatusResultDTO dto2 = new ToolStatusResultDTO(
                "done", "t-1", "tool-a", "{}", true, "ok", "msg", true, brief2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toString包含toolConfig字段() {
        ToolStatusResultDTO.ToolConfigBrief brief = createBrief("42", "BROWSER", "my_tool");
        ToolStatusResultDTO dto = new ToolStatusResultDTO(
                "done", "t-1", "tool-a", "{}", true, "ok", "msg", true, brief);
        String str = dto.toString();
        assertTrue(str.contains("toolConfig"));
    }

    @Test
    void ToolConfigBrief默认值() {
        ToolStatusResultDTO.ToolConfigBrief brief = new ToolStatusResultDTO.ToolConfigBrief();
        assertNull(brief.getId());
        assertNull(brief.getSubToolType());
        assertNull(brief.getToolName());
    }

    @Test
    void ToolConfigBriefsetterAndGetter() {
        ToolStatusResultDTO.ToolConfigBrief brief = new ToolStatusResultDTO.ToolConfigBrief();
        brief.setId("100");
        brief.setSubToolType("BROWSER");
        brief.setToolName("browser_tool");
        assertEquals("100", brief.getId());
        assertEquals("BROWSER", brief.getSubToolType());
        assertEquals("browser_tool", brief.getToolName());
    }

    @Test
    void ToolConfigBriefequalsAndHashCode() {
        ToolStatusResultDTO.ToolConfigBrief brief1 = createBrief("1", "BROWSER", "t1");
        ToolStatusResultDTO.ToolConfigBrief brief2 = createBrief("1", "BROWSER", "t1");
        assertEquals(brief1, brief2);
        assertEquals(brief1.hashCode(), brief2.hashCode());
    }

    @Test
    void ToolConfigBrieftoString() {
        ToolStatusResultDTO.ToolConfigBrief brief = createBrief("1", "BROWSER", "t1");
        String str = brief.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("BROWSER"));
        assertTrue(str.contains("t1"));
    }
}