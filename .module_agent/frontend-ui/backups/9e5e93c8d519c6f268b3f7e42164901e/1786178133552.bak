import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const pagePath = resolve(__dirname, '../ConversationDetail.tsx');

describe('ConversationDetail 对话详情 (静态验证)', () => {
  it('应导入并调用 getConversationMessages', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("getConversationMessages");
    expect(source).toContain("getConversationMessages(cid)");
  });

  it('应从 useParams 取 conversationId 并触发 fetchMessages', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('conversationId');
    expect(source).toContain('fetchMessages(conversationId)');
  });

  it('应按角色展示 Tag（用户/助手/工具/系统）', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("ROLE_LABELS");
    expect(source).toContain("user: { text: '用户'");
    expect(source).toContain("assistant: { text: '助手'");
    expect(source).toContain("tool: { text: '工具'");
    expect(source).toContain("system: { text: '系统'");
    expect(source).toContain("<Tag");
  });

  it('应展示内容与时间列', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("dataIndex: 'content'");
    expect(source).toContain("dataIndex: 'createTime'");
  });

  it('应包含返回按钮（返回），从 state 取 sessionId 返回 /conversations/:sessionId', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('返回');
    expect(source).toContain('useLocation');
    expect(source).toContain('location.state');
    expect(source).toContain('`/conversations/${sessionId}`');
    expect(source).toContain("'/conversations'");
  });

  it('assistant 有 toolCalls 时应显示「查看工具」按钮和数量', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("record.role === 'assistant'");
    expect(source).toContain('record.toolCalls');
    expect(source).toContain('查看工具');
    expect(source).toContain('toolCalls.length');
  });

  it('tool 角色应显示「查看结果」按钮', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("record.role === 'tool'");
    expect(source).toContain('查看结果');
  });

  it('应通过 Modal 展示 toolCalls/toolResult 的 JSON.stringify 内容', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('Modal');
    expect(source).toContain('JSON.stringify(toolCalls, null, 2)');
    expect(source).toContain('JSON.stringify(toolResult ?? null, null, 2)');
    expect(source).toContain('detailVisible');
  });

  it('应包含来源会话列并截短显示 sessionId（前8后4）', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("title: '来源会话'");
    expect(source).toContain("dataIndex: 'sessionId'");
    expect(source).toContain('shortenSessionId');
    expect(source).toContain('id.slice(0, 8)');
    expect(source).toContain('id.slice(-4)');
  });

  it('应使用 rowClassName 按 sessionId 是否等于主会话设置不同背景色', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('rowClassName={rowClassName}');
    expect(source).toContain("record.sessionId === sessionId");
    expect(source).toContain("'conversation-main-row'");
    expect(source).toContain("'conversation-child-row'");
  });
});
