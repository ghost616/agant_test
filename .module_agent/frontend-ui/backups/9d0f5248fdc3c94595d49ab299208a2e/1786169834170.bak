import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const pagePath = resolve(__dirname, '../ConversationHistory.tsx');
const appPath = resolve(__dirname, '../../../App.tsx');

describe('ConversationHistory 会话历史 (静态验证)', () => {
  it('应导入 listSessions 与 getSessionMessages', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('listSessions');
    expect(source).toContain('getSessionMessages');
  });

  it('/conversations 主列表应复用 listSessions()', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('listSessions()');
  });

  it('/conversations/:sessionId 应过滤 role===\'user\' 展示用户消息', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain("msg.role === 'user'");
  });

  it('有 conversationId 时应显示「查看详情」按钮并跳转 /conversations/:conversationId/detail', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('record.conversationId');
    expect(source).toContain('查看详情');
    expect(source).toContain('`/conversations/${record.conversationId}/detail`');
  });

  it('会话列表操作应跳转 /conversations/:sessionId', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('`/conversations/${record.id}`');
    expect(source).toContain('查看消息');
  });

  it('应包含返回按钮（返回会话列表）', () => {
    const source = readFileSync(pagePath, 'utf-8');
    expect(source).toContain('返回会话列表');
    expect(source).toContain("navigate('/conversations')");
  });
});

describe('App 会话历史路由与菜单 (静态验证)', () => {
  it('应导入 ConversationHistory 与 ConversationDetail', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("import ConversationHistory from './pages/sessions/ConversationHistory'");
    expect(source).toContain("import ConversationDetail from './pages/sessions/ConversationDetail'");
  });

  it('应注册 /conversations 路由', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('path="/conversations" element={<ConversationHistory />}');
  });

  it('应注册 /conversations/:sessionId 路由', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('path="/conversations/:sessionId" element={<ConversationHistory />}');
  });

  it('应注册 /conversations/:conversationId/detail 路由', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      'path="/conversations/:conversationId/detail" element={<ConversationDetail />}',
    );
  });

  it('应包含「会话历史」菜单项并带 HistoryOutlined 图标', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("key: '/conversations'");
    expect(source).toContain('label: \'会话历史\'');
    expect(source).toContain('HistoryOutlined');
  });
});
