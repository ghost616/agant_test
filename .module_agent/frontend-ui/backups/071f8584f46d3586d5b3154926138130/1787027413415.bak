import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  webSocketClient,
  WS_ENDPOINT,
  WS_TYPE_BIND,
  WS_TYPE_UNBIND,
  WS_MESSAGE_NAME_BIND_RESULT,
  WS_MESSAGE_NAME_SEND_USER_MESSAGE,
} from '../websocket';

/**
 * 伪 WebSocket：记录实例与发送消息，测试中手动触发 open/close/message。
 */
class MockWebSocket {
  static instances: MockWebSocket[] = [];
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = MockWebSocket.CONNECTING;
  sent: string[] = [];
  onopen: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  onclose: (() => void) | null = null;
  url: string;

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  send(data: string): void {
    this.sent.push(data);
  }

  close(): void {
    this.readyState = MockWebSocket.CLOSED;
  }

  /** 模拟服务端完成握手：进入 OPEN 状态并触发 onopen。 */
  open(): void {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.();
  }

  /** 模拟收到服务端消息。 */
  receive(data: string): void {
    this.onmessage?.({ data });
  }

  /** 模拟连接被服务端/网络断开：进入 CLOSED 并触发 onclose。 */
  serverClose(): void {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.();
  }
}

const BIND = (sessionId: string) => JSON.stringify({ type: 'BIND', sessionId });
const UNBIND = (sessionId: string) => JSON.stringify({ type: 'UNBIND', sessionId });

beforeEach(() => {
  MockWebSocket.instances = [];
  webSocketClient.close();
  vi.stubGlobal('WebSocket', MockWebSocket);
  vi.stubEnv('MODE', 'development');
  vi.useFakeTimers();
});

afterEach(() => {
  webSocketClient.close();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

describe('websocket 常量', () => {
  it('导出与后端约定的常量', () => {
    expect(WS_ENDPOINT).toBe('/ws');
    expect(WS_TYPE_BIND).toBe('BIND');
    expect(WS_TYPE_UNBIND).toBe('UNBIND');
    expect(WS_MESSAGE_NAME_BIND_RESULT).toBe('BIND_RESULT');
    expect(WS_MESSAGE_NAME_SEND_USER_MESSAGE).toBe('SEND_USER_MESSAGE');
  });
});

describe('connect', () => {
  it('测试模式（MODE=test）下静默跳过，不建立连接', () => {
    vi.stubEnv('MODE', 'test');
    webSocketClient.connect();
    expect(MockWebSocket.instances).toHaveLength(0);
  });

  it('环境不支持 WebSocket 时静默跳过，不抛错', () => {
    vi.stubGlobal('WebSocket', undefined);
    expect(() => webSocketClient.connect()).not.toThrow();
  });

  it('非测试模式下建立到 /ws 端点的连接', () => {
    webSocketClient.connect();
    expect(MockWebSocket.instances).toHaveLength(1);
    expect(MockWebSocket.instances[0].url).toBe(`ws://${window.location.host}/ws`);
  });

  it('连接中重复调用不重复建连（幂等）', () => {
    webSocketClient.connect();
    webSocketClient.connect();
    webSocketClient.connect();
    expect(MockWebSocket.instances).toHaveLength(1);
  });

  it('已连接时重复调用不重复建连（幂等）', () => {
    webSocketClient.connect();
    MockWebSocket.instances[0].open();
    expect(webSocketClient.connected).toBe(true);
    webSocketClient.connect();
    expect(MockWebSocket.instances).toHaveLength(1);
  });
});

describe('bindSession / unbindSession', () => {
  it('连接未就绪时先建立连接，open 后补发 BIND', () => {
    webSocketClient.bindSession('s1');
    expect(MockWebSocket.instances).toHaveLength(1);
    const ws = MockWebSocket.instances[0];
    expect(ws.sent).toHaveLength(0);
    ws.open();
    expect(ws.sent).toEqual([BIND('s1')]);
  });

  it('已连接时 bindSession 直接发送 BIND', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.bindSession('s1');
    expect(ws.sent).toEqual([BIND('s1')]);
  });

  it('切换绑定先发送 UNBIND 再发送 BIND（只更新绑定不断链）', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.bindSession('s1');
    webSocketClient.bindSession('s2');
    expect(ws.sent).toEqual([BIND('s1'), UNBIND('s1'), BIND('s2')]);
    expect(MockWebSocket.instances).toHaveLength(1);
  });

  it('相同会话重复 bindSession 不发送 UNBIND', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.bindSession('s1');
    webSocketClient.bindSession('s1');
    expect(ws.sent).toEqual([BIND('s1'), BIND('s1')]);
    expect(ws.sent.some((m) => m.includes('UNBIND'))).toBe(false);
  });

  it('unbindSession 发送 UNBIND 并清除本地绑定', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.bindSession('s1');
    ws.sent.length = 0;
    webSocketClient.unbindSession('s1');
    expect(ws.sent).toEqual([UNBIND('s1')]);
    // 解绑后再触发 open 不应补发 BIND
    ws.open();
    expect(ws.sent).toEqual([UNBIND('s1')]);
  });

  it('unbindSession 在连接未就绪时不发送消息仅清除绑定', () => {
    webSocketClient.bindSession('s1');
    const ws = MockWebSocket.instances[0];
    webSocketClient.unbindSession('s1');
    expect(ws.sent).toHaveLength(0);
  });
});

describe('close', () => {
  it('手动关闭后调用底层 close 并清除绑定', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.bindSession('s1');
    webSocketClient.close();
    expect(ws.readyState).toBe(MockWebSocket.CLOSED);
    // 手动关闭后 ws 引用被置空：再次连接会创建新实例
    webSocketClient.connect();
    expect(MockWebSocket.instances).toHaveLength(2);
  });

  it('关闭后不再自动重连', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    webSocketClient.close();
    ws.serverClose();
    vi.advanceTimersByTime(60000);
    expect(MockWebSocket.instances).toHaveLength(1);
  });
});

describe('断线自动重连（指数退避）', () => {
  it('非手动关闭触发重连，退避起点 1s', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    ws.serverClose();
    vi.advanceTimersByTime(999);
    expect(MockWebSocket.instances).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(MockWebSocket.instances).toHaveLength(2);
  });

  it('连续失败退避递增 1s→2s→4s→8s→16s，上限 30s', () => {
    webSocketClient.connect();
    let ws = MockWebSocket.instances[0];
    ws.open();
    const delays = [1000, 2000, 4000, 8000, 16000, 30000, 30000];
    for (let i = 0; i < delays.length; i++) {
      // 模拟连续失败：重连产生的新连接不再 open，直接再次断开
      ws.serverClose();
      vi.advanceTimersByTime(delays[i] - 1);
      expect(MockWebSocket.instances).toHaveLength(i + 1);
      vi.advanceTimersByTime(1);
      expect(MockWebSocket.instances).toHaveLength(i + 2);
      ws = MockWebSocket.instances[i + 1];
    }
    // 达到上限后保持 30s
    ws.serverClose();
    vi.advanceTimersByTime(29999);
    expect(MockWebSocket.instances).toHaveLength(delays.length + 1);
    vi.advanceTimersByTime(1);
    expect(MockWebSocket.instances).toHaveLength(delays.length + 2);
  });

  it('重连成功（onopen）后重新发送当前绑定的 BIND', () => {
    webSocketClient.connect();
    const ws0 = MockWebSocket.instances[0];
    ws0.open();
    webSocketClient.bindSession('s1');
    ws0.serverClose();
    vi.advanceTimersByTime(1000);
    const ws1 = MockWebSocket.instances[1];
    expect(ws1.sent).toHaveLength(0);
    ws1.open();
    expect(ws1.sent).toEqual([BIND('s1')]);
  });

  it('重连成功后将退避计数重置（下次断线仍从 1s 起）', () => {
    webSocketClient.connect();
    let ws = MockWebSocket.instances[0];
    ws.open();
    ws.serverClose();
    vi.advanceTimersByTime(1000);
    ws = MockWebSocket.instances[1];
    ws.open();
    ws.serverClose();
    vi.advanceTimersByTime(999);
    expect(MockWebSocket.instances).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(MockWebSocket.instances).toHaveLength(3);
  });
});

describe('onMessage / offMessage', () => {
  it('JSON 消息解析后分发给监听器', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    const listener = vi.fn();
    webSocketClient.onMessage(listener);
    ws.receive(JSON.stringify({ messageName: 'SEND_USER_MESSAGE', sessionId: 'c1' }));
    expect(listener).toHaveBeenCalledWith({ messageName: 'SEND_USER_MESSAGE', sessionId: 'c1' });
    webSocketClient.offMessage(listener);
  });

  it('非 JSON 消息静默忽略', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    const listener = vi.fn();
    webSocketClient.onMessage(listener);
    ws.receive('not-json{');
    expect(listener).not.toHaveBeenCalled();
    webSocketClient.offMessage(listener);
  });

  it('单个监听器异常不影响其他监听器', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    const bad = vi.fn(() => {
      throw new Error('boom');
    });
    const good = vi.fn();
    webSocketClient.onMessage(bad);
    webSocketClient.onMessage(good);
    ws.receive(JSON.stringify({ a: 1 }));
    expect(bad).toHaveBeenCalled();
    expect(good).toHaveBeenCalled();
    webSocketClient.offMessage(bad);
    webSocketClient.offMessage(good);
  });

  it('offMessage 后不再分发', () => {
    webSocketClient.connect();
    const ws = MockWebSocket.instances[0];
    ws.open();
    const listener = vi.fn();
    webSocketClient.onMessage(listener);
    webSocketClient.offMessage(listener);
    ws.receive(JSON.stringify({ a: 1 }));
    expect(listener).not.toHaveBeenCalled();
  });
});
