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
});
