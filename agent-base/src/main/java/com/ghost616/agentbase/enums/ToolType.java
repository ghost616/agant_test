package com.ghost616.agentbase.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 工具实现类型枚举。
 */
public enum ToolType {

    JAVA("JAVA", "Java接口实现"),
    TYPESCRIPT("TYPESCRIPT", "TypeScript脚本"),
    PYTHON("PYTHON", "Python脚本"),
    MCP_HTTP("MCP_HTTP", "MCP HTTP远程工具");

    @EnumValue
    private final String code;
    private final String description;

    ToolType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
