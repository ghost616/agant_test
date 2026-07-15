import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('AgentList SessionAuthSelect 组件 (静态验证)', () => {
  it('应定义 SESSION_AUTH_LABELS 映射', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('ALL: \'所有会话\'');
    expect(source).toContain('PARENT: \'父会话\'');
    expect(source).toContain('CHILD: \'子会话\'');
  });

  it('应定义 SESSION_AUTH_COLORS 颜色映射', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('ALL: \'blue\'');
    expect(source).toContain('PARENT: \'green\'');
    expect(source).toContain('CHILD: \'orange\'');
  });

  it('应定义 SESSION_AUTH_OPTIONS', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('SESSION_AUTH_OPTIONS');
  });

  it('SessionAuthSelect 函数应处理 value 和 onChange', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    const funcBlock = source.match(/function SessionAuthSelect[\s\S]*?\n\}/);
    expect(funcBlock).not.toBeNull();
    if (funcBlock) {
      expect(funcBlock[0]).toContain('value');
      expect(funcBlock[0]).toContain('onChange');
    }
  });

  it('SessionAuthSelect 应包含 idField 参数 (toolId | skillId)', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("idField: 'toolId' | 'skillId'");
  });

  it('handleSelectChange 新增项默认 sessionAuth=ALL', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("sessionAuth: valueMap[id] || ('ALL' as SessionAuthType)");
  });

  it('handleAuthChange 应更新对应项的 sessionAuth', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('handleAuthChange');
    const handleBlock = source.match(/const handleAuthChange[\s\S]*?};/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain('v[idField] === id ? { ...v, sessionAuth } : v');
    }
  });

  it('SessionAuthSelect 应渲染 Select 和多选已选项的 sessionAuth 下拉', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('mode="multiple"');
    expect(source).toContain('allowClear');
    expect(source).toContain('showSearch');
  });

  it('已选项应显示名称标签和 sessionAuth 下拉选择框', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('<Tag>');
    expect(source).toContain('handleAuthChange(id, v as SessionAuthType)');
    expect(source).toContain('SESSION_AUTH_OPTIONS');
  });

  it('移除项时同步从数据中删除', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    const handleSelectBlock = source.match(/const handleSelectChange[\s\S]*?};/);
    expect(handleSelectBlock).not.toBeNull();
    if (handleSelectBlock) {
      expect(handleSelectBlock[0]).toContain('ids.map');
      expect(handleSelectBlock[0]).toContain('sessionAuth');
    }
  });
});

describe('AgentList 表单交互 (静态验证)', () => {
  it('新增时表单重置，editingAgent 为 null', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('setEditingAgent(null)');
    expect(source).toContain('form.resetFields()');
  });

  it('编辑时回填 tools/skills 数据', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('tools: editingAgent.tools');
    expect(source).toContain('skills: editingAgent.skills');
  });

  it('表单提交时 tools/skills 使用 AgentFormData 结构', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('Form.useForm<AgentFormData>()');
    expect(source).toContain("Form.Item name=\"tools\"");
    expect(source).toContain("Form.Item name=\"skills\"");
  });

  it('handleModalOk 应调用 updateAgent 或 createAgent', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    const modalOkBlock = source.match(/const handleModalOk[\s\S]*?};/);
    expect(modalOkBlock).not.toBeNull();
    if (modalOkBlock) {
      expect(modalOkBlock[0]).toContain('updateAgent(editingAgent.id, values)');
      expect(modalOkBlock[0]).toContain('createAgent(values)');
    }
  });
});

describe('AgentList 表格展示 (静态验证)', () => {
  it('\"挂载工具\"列应渲染 tools 数据', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("title: '挂载工具'");
    expect(source).toContain("dataIndex: 'tools'");
  });

  it('\"挂载技能\"列应渲染 skills 数据', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("title: '挂载技能'");
    expect(source).toContain("dataIndex: 'skills'");
  });

  it('工具列每个 Tag 显示工具名和 sessionAuth 标签', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('SESSION_AUTH_COLORS[item.sessionAuth]');
    expect(source).toContain('SESSION_AUTH_LABELS[item.sessionAuth]');
  });

  it('技能列每个 Tag 显示技能名和 sessionAuth 标签', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain('SESSION_AUTH_COLORS[item.sessionAuth]');
    expect(source).toContain('SESSION_AUTH_LABELS[item.sessionAuth]');
  });

  it('空值时显示 \'-\'', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("if (!value || value.length === 0) return '-'");
  });

  it('tools 和 skills 列使用 Space wrap 布局', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    const toolsRenderBlock = source.match(/render: \(value:.*toolId.*\) => [\s\S]*?},/);
    expect(toolsRenderBlock).not.toBeNull();
    if (toolsRenderBlock) {
      expect(toolsRenderBlock[0]).toContain('<Space');
      expect(toolsRenderBlock[0]).toContain('wrap');
    }
  });

  it('sessionAuth 使用不同颜色区分 (blue/green/orange)', () => {
    const source = readFileSync(resolve(__dirname, '../AgentList.tsx'), 'utf-8');
    expect(source).toContain("'blue'");
    expect(source).toContain("'green'");
    expect(source).toContain("'orange'");
  });
});
