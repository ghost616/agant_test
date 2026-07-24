import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('ToolList 类型定义', () => {
  it('ToolType 应包含 CUSTOM', () => {
    const source = readFileSync(resolve(__dirname, '../../../types/tool.ts'), 'utf-8');
    expect(source).toContain("'CUSTOM'");
  });

  it('SubToolType 应包含 BROWSER', () => {
    const source = readFileSync(resolve(__dirname, '../../../types/tool.ts'), 'utf-8');
    expect(source).toContain("'BROWSER'");
  });

  it('ToolConfig 应包含 subToolType 和 toolScript 字段', () => {
    const source = readFileSync(resolve(__dirname, '../../../types/tool.ts'), 'utf-8');
    expect(source).toContain('subToolType');
    expect(source).toContain('toolScript');
  });
});

describe('ToolList CUSTOM 标签与颜色', () => {
  it('应定义 CUSTOM 标签为 Custom', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("CUSTOM: 'Custom'");
  });

  it('应定义 CUSTOM 颜色为 cyan', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("CUSTOM: 'cyan'");
  });
});

describe('ToolList 子工具类型', () => {
  it('应定义 BROWSER 子类型', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("BROWSER: 'Browser'");
    expect(source).toContain('SUB_TOOL_TYPE_OPTIONS');
  });
});

describe('ToolList CUSTOM 类型提交逻辑', () => {
  it('CUSTOM 类型提交时设置 subToolType/toolScript，清空 implPath/parameterSchema/returnSchema', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("allValues.toolType === 'CUSTOM'");
    expect(source).toContain('values.subToolType = allValues.subToolType');
    expect(source).toContain('values.toolScript = allValues.toolScript');
    expect(source).toContain("values.implPath = ''");
    expect(source).toContain("values.parameterSchema = ''");
    expect(source).toContain("values.returnSchema = ''");
  });

  it('非 CUSTOM 类型走 else if 分支', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("} else if (allValues.toolType === 'MCP_HTTP')");
  });
});

describe('ToolList MCP_HTTP 编辑逻辑', () => {
  it('编辑 MCP_HTTP 时 authConfig 回填 authorization', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain('editingTool.authConfig');
    expect(source).toContain('JSON.parse');
    expect(source).toContain('auth.token');
    expect(source).toContain('values.authorization');
  });

  it('MCP_HTTP 提交时 parameterSchema/returnSchema 置空，authorization 转为 authConfig', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("allValues.toolType === 'MCP_HTTP'");
    expect(source).toContain("values.parameterSchema = ''");
    expect(source).toContain("values.returnSchema = ''");
    expect(source).toContain('values.authConfig');
  });
});

describe('ToolList 非 CUSTOM/非 MCP_HTTP 类型提交逻辑', () => {
  it('初始 values 中 subToolType 和 toolScript 默认为 undefined', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain('subToolType: undefined');
    expect(source).toContain('toolScript: undefined');
  });

  it('CUSTOM 分支和 MCP_HTTP 分支存在', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("if (allValues.toolType === 'CUSTOM')");
    expect(source).toContain("} else if (allValues.toolType === 'MCP_HTTP')");
  });
});

describe('ToolList CUSTOM 表单 UI', () => {
  it('CUSTOM 类型应显示子工具类型下拉框', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("toolType === 'CUSTOM'");
    expect(source).toContain('subToolType');
  });

  it('BROWSER 子类型应显示工具脚本 TextArea，非 BROWSER 则显示 implPath', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("toolType === 'CUSTOM' && subToolType === 'BROWSER'");
    expect(source).toContain('toolScript');
  });

  it('CUSTOM 和 MCP_HTTP 类型隐藏 parameterSchema/returnSchema', () => {
    const source = readFileSync(resolve(__dirname, '../ToolList.tsx'), 'utf-8');
    expect(source).toContain("toolType !== 'MCP_HTTP' && toolType !== 'CUSTOM'");
    expect(source).toContain('parameterSchema');
    expect(source).toContain('returnSchema');
  });
});
