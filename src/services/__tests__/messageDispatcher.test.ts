import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import type { SendUserMessagePayload } from '../messageDispatcher';

/** 受控 mock 的 webSocketClient：捕获模块加载时注册的消息回调，测试中手动触发。 */
const mockClient = vi.hoisted(() => {
  let listener: ((payload: Record<string, unknown>) => void) | null = null;
  return {
    onMessage: vi.fn((cb: (payload: Record<string, unknown>) => void) => {
      listener = cb;
    }),
    _dispatch: vi.fn((payload: Record<string, unknown>) => {
      listener?.(payload);
    }),
  };
});

vi.mock('../websocket', () => ({
  webSocketClient: mockClient,
  WS_MESSAGE_NAME_SEND_USER_MESSAGE: 'SEND_USER_MESSAGE',
}));

import {
  SEND_USER_MESSAGE_MARKER,
  registerSessionPage,
  unregisterSessionPage,
  subscribeChildSessionsChanged,
  unsubscribeChildSessionsChanged,
  handleSendUserMessage,
} from '../messageDispatcher';

interface Handler {
  mainSessionId: string;
  isChildActive: Mock<(childSessionId: string) => boolean>;
  streamChildReply: Mock<(message: SendUserMessagePayload) => void>;
  refreshChildSessions: Mock<() => void>;
}

function makeHandler(mainSessionId: string, active = true): Handler {
  return {
    mainSessionId,
    isChildActive: vi.fn((_childId: string) => active),
    streamChildReply: vi.fn((_message: SendUserMessagePayload) => {}),
    refreshChildSessions: vi.fn(() => {}),
  };
}

beforeEach(() => {
  // 注意：不清 mockClient.onMessage —— 模块加载时注册订阅的调用记录需保留用于断言
  mockClient._dispatch.mockClear();
});

describe('messageDispatcher 常量', () => {
  it('SEND_USER_MESSAGE_MARKER 与后端 ChatService 约定一致', () => {
    expect(SEND_USER_MESSAGE_MARKER).toBe('[send_user_message]');
  });
});

describe('模块级订阅', () => {
  it('模块加载时订阅 webSocketClient.onMessage', () => {
    expect(mockClient.onMessage).toHaveBeenCalledTimes(1);
  });

  it('仅 messageName === SEND_USER_MESSAGE 的消息才分发', () => {
    const handler = makeHandler('m1');
    registerSessionPage(handler);
    // 非 SEND_USER_MESSAGE 消息：不触发任何页面处理
    mockClient._dispatch({ messageName: 'OTHER', sessionId: 'c1', mainSessionId: 'm1' });
    expect(handler.streamChildReply).not.toHaveBeenCalled();
    expect(handler.refreshChildSessions).not.toHaveBeenCalled();
    // SEND_USER_MESSAGE 消息：命中并激活 → streamChildReply
    mockClient._dispatch({
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      mainSessionId: 'm1',
      content: '',
    });
    expect(handler.streamChildReply).toHaveBeenCalledTimes(1);
    unregisterSessionPage('m1');
  });
});

describe('handleSendUserMessage 分发', () => {
  it('未命中页面且无监听器时不抛错', () => {
    expect(() =>
      handleSendUserMessage({ messageName: 'SEND_USER_MESSAGE', sessionId: 'x', content: '' }),
    ).not.toThrow();
  });

  it('未命中页面时触发 childSessionsChanged 监听器', () => {
    const listener = vi.fn((_payload: SendUserMessagePayload) => {});
    subscribeChildSessionsChanged(listener);
    const msg: SendUserMessagePayload = {
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      content: '',
    };
    handleSendUserMessage(msg);
    expect(listener).toHaveBeenCalledWith(msg);
    unsubscribeChildSessionsChanged(listener);
  });

  it('unsubscribeChildSessionsChanged 后不再触发', () => {
    const listener = vi.fn((_payload: SendUserMessagePayload) => {});
    subscribeChildSessionsChanged(listener);
    unsubscribeChildSessionsChanged(listener);
    handleSendUserMessage({ messageName: 'SEND_USER_MESSAGE', sessionId: 'c1', content: '' });
    expect(listener).not.toHaveBeenCalled();
  });

  it('mainSessionId 匹配且子会话激活 → 调用 streamChildReply', () => {
    const handler = makeHandler('m1', true);
    registerSessionPage(handler);
    const msg: SendUserMessagePayload = {
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      mainSessionId: 'm1',
      content: '',
    };
    handleSendUserMessage(msg);
    expect(handler.isChildActive).toHaveBeenCalledWith('c1');
    expect(handler.streamChildReply).toHaveBeenCalledWith(msg);
    expect(handler.refreshChildSessions).not.toHaveBeenCalled();
    unregisterSessionPage('m1');
  });

  it('parentSessionId 匹配 → 命中页面', () => {
    const handler = makeHandler('m1', true);
    registerSessionPage(handler);
    const msg: SendUserMessagePayload = {
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      parentSessionId: 'm1',
      content: '',
    };
    handleSendUserMessage(msg);
    expect(handler.streamChildReply).toHaveBeenCalledWith(msg);
    unregisterSessionPage('m1');
  });

  it('sessionId 匹配 → 命中页面（子会话自身即主会话）', () => {
    const handler = makeHandler('m1', true);
    registerSessionPage(handler);
    const msg: SendUserMessagePayload = {
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'm1',
      content: '',
    };
    handleSendUserMessage(msg);
    expect(handler.streamChildReply).toHaveBeenCalledWith(msg);
    unregisterSessionPage('m1');
  });

  it('命中但子会话非激活 → 调用 refreshChildSessions', () => {
    const handler = makeHandler('m1', false);
    registerSessionPage(handler);
    handleSendUserMessage({
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      mainSessionId: 'm1',
      content: '',
    });
    expect(handler.streamChildReply).not.toHaveBeenCalled();
    expect(handler.refreshChildSessions).toHaveBeenCalled();
    unregisterSessionPage('m1');
  });

  it('message.sessionId 为空时不调用 isChildActive，而是刷新子会话列表', () => {
    const handler = makeHandler('m1', true);
    registerSessionPage(handler);
    handleSendUserMessage({
      messageName: 'SEND_USER_MESSAGE',
      sessionId: '',
      mainSessionId: 'm1',
      content: '',
    });
    expect(handler.isChildActive).not.toHaveBeenCalled();
    expect(handler.streamChildReply).not.toHaveBeenCalled();
    expect(handler.refreshChildSessions).toHaveBeenCalled();
    unregisterSessionPage('m1');
  });

  it('unregisterSessionPage 后不再命中，转由 childSessionsChanged 处理', () => {
    const handler = makeHandler('m1', true);
    registerSessionPage(handler);
    unregisterSessionPage('m1');
    const listener = vi.fn((_payload: SendUserMessagePayload) => {});
    subscribeChildSessionsChanged(listener);
    handleSendUserMessage({
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      mainSessionId: 'm1',
      content: '',
    });
    expect(handler.streamChildReply).not.toHaveBeenCalled();
    expect(listener).toHaveBeenCalled();
    unsubscribeChildSessionsChanged(listener);
  });

  it('重复注册同一 mainSessionId 时后注册的处理器生效（覆盖）', () => {
    const h1 = makeHandler('m1', true);
    const h2 = makeHandler('m1', true);
    registerSessionPage(h1);
    registerSessionPage(h2);
    handleSendUserMessage({
      messageName: 'SEND_USER_MESSAGE',
      sessionId: 'c1',
      mainSessionId: 'm1',
      content: '',
    });
    expect(h1.streamChildReply).not.toHaveBeenCalled();
    expect(h2.streamChildReply).toHaveBeenCalled();
    unregisterSessionPage('m1');
  });

  it('单个 childSessionsChanged 监听器异常不影响其他监听器', () => {
    const bad = vi.fn((_payload: SendUserMessagePayload) => {
      throw new Error('boom');
    });
    const good = vi.fn((_payload: SendUserMessagePayload) => {});
    subscribeChildSessionsChanged(bad);
    subscribeChildSessionsChanged(good);
    handleSendUserMessage({ messageName: 'SEND_USER_MESSAGE', sessionId: 'c1', content: '' });
    expect(good).toHaveBeenCalled();
    unsubscribeChildSessionsChanged(bad);
    unsubscribeChildSessionsChanged(good);
  });
});
