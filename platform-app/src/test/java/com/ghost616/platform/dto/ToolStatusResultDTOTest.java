package com.ghost616.platform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolStatusResultDTOTest {

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
    }

    @Test
    void 全参构造器正确设置所有字段() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO(
                "done", "tool-001", "file_reader", "--path=test.txt", true, "success result", "ok", true);

        assertEquals("done", dto.getStatus());
        assertEquals("tool-001", dto.getToolId());
        assertEquals("file_reader", dto.getToolName());
        assertEquals("--path=test.txt", dto.getArguments());
        assertTrue(dto.isHasMore());
        assertEquals("success result", dto.getResult());
        assertEquals("ok", dto.getMessage());
        assertTrue(dto.isNeedsSubSessionFlow());
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

        assertEquals("running", dto.getStatus());
        assertEquals("tc-99", dto.getToolId());
        assertEquals("code_runner", dto.getToolName());
        assertEquals("{\"lang\":\"java\"}", dto.getArguments());
        assertTrue(dto.isHasMore());
        assertEquals("output", dto.getResult());
        assertEquals("completed", dto.getMessage());
        assertTrue(dto.isNeedsSubSessionFlow());
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
    }

    @Test
    void 全参构造器接收空值() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO(null, null, null, null, false, null, null, false);
        assertNull(dto.getStatus());
        assertNull(dto.getToolId());
        assertNull(dto.getToolName());
        assertNull(dto.getArguments());
        assertFalse(dto.isHasMore());
        assertNull(dto.getResult());
        assertNull(dto.getMessage());
        assertFalse(dto.isNeedsSubSessionFlow());
    }

    @Test
    void equals和hashCode一致性() {
        ToolStatusResultDTO dto1 = new ToolStatusResultDTO(
                "idle", "t-1", "tool-a", "", false, null, null, false);
        ToolStatusResultDTO dto2 = new ToolStatusResultDTO(
                "idle", "t-1", "tool-a", "", false, null, null, false);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void equals不同对象不等() {
        ToolStatusResultDTO dto1 = new ToolStatusResultDTO(
                "done", "t-1", "tool-a", "{}", true, "ok", "msg", true);
        ToolStatusResultDTO dto2 = new ToolStatusResultDTO(
                "fail", "t-1", "tool-a", "{}", true, "ok", "msg", true);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toString包含关键字段() {
        ToolStatusResultDTO dto = new ToolStatusResultDTO(
                "done", "t-1", "tool-a", "{}", true, "ok", "msg", true);
        String str = dto.toString();
        assertTrue(str.contains("done"));
        assertTrue(str.contains("t-1"));
        assertTrue(str.contains("tool-a"));
    }
}
