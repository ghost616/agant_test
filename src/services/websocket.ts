/**
 * 全局 WebSocket 客户端（单例）。
 *
 * <p>登录后建立一条到后端 /ws 端点的连接（复用后端 SESSION_ID Cookie 完成握手鉴权），
 * 提供会话绑定/解绑消息（{@code {type:'BIND'|'UNBIND', sessionId}}）与断线自动重连
 * （指数退避，重连成功后自动重新发送当前绑定的会话绑定消息）。</p>
 */

/** WebSocket 端点路径（与后端 WebSocketConfig.WS_ENDPOINT 约定一致）。 */
export const WS_ENDPOINT = '/ws';

/** 绑定会话入站消息类型（与后端 SessionWebSocketHandler.TYPE_BIND 一致）。 */
export const WS_TYPE_BIND = 'BIND';

/** 解绑会话入站消息类型（与后端 SessionWebSocketHandler.TYPE_UNBIND 一致）。 */
export const WS_TYPE_UNBIND = 'UNBIND';

/** 绑定结果回执消息名（与后端 SessionWebSocketHandler.MESSAGE_NAME_BIND_RESULT 一致）。 */
export const WS_MESSAGE_NAME_BIND_RESULT = 'BIND_RESULT';

/** 服务端推送的 SEND_USER_MESSAGE 消息名（与后端 MessageName.SEND_USER_MESSAGE 一致）。 */
export const WS_MESSAGE_NAME_SEND_USER_MESSAGE = 'SEND_USER_MESSAGE';

/** 重连退避基数（毫秒）。 */
const RECONNECT_BASE_DELAY = 1000;

/** 重连退避上限（毫秒）。 */
const RECONNECT_MAX_DELAY = 30000;

/** WebSocket 消息监听器。 */
export type WsMessageListener = (payload: Record<string, unknown>) => void;

/**
 * 构建 WebSocket 连接地址：当前站点协议 + 主机 + /ws。
 * 开发环境经 Vite /ws 代理转发到后端，生产环境由部署层代理（见 vite.config.ts / serve.js）。
 */
function buildWebSocketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${WS_ENDPOINT}`;
}

/**
 * 全局 WebSocket 客户端单例：管理连接生命周期、会话绑定与断线重连。
 */
class WebSocketClient {
  private ws: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts = 0;
  private manualClosed = false;
  private boundSessionId: string | null = null;
  private listeners = new Set<WsMessageListener>();

  /** 当前连接是否已建立。 */
  get connected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  /**
   * 建立 WebSocket 连接（幂等）：已在连接中或已连接时直接返回；
   * 环境不支持 WebSocket（如 jsdom 单测）或测试模式（vitest）时静默跳过。
   */
  connect(): void {
    if (typeof window === 'undefined' || typeof WebSocket === 'undefined') {
      return;
    }
    if (import.meta.env.MODE === 'test') {
      return;
    }
    const state = this.ws?.readyState;
    if (state === WebSocket.OPEN || state === WebSocket.CONNECTING) {
      return;
    }
    this.manualClosed = false;
    try {
      const ws = new WebSocket(buildWebSocketUrl());
      this.ws = ws;
      ws.onopen = () => this.handleOpen();
      ws.onmessage = (event) => this.handleMessage(event);
      ws.onclose = () => this.handleClose();
      // onerror 后必然触发 onclose，统一由 handleClose 调度重连
    } catch {
      this.scheduleReconnect();
    }
  }

  /**
   * 手动关闭连接（退出登录时调用）：清除重连定时器并清空绑定。
   */
  close(): void {
    this.manualClosed = true;
    this.clearReconnectTimer();
    this.boundSessionId = null;
    if (this.ws) {
      try {
        this.ws.close();
      } catch {
        // 忽略关闭异常
      }
      this.ws = null;
    }
  }

  /**
   * 绑定当前会话：记录会话 ID 并发送 BIND 消息；
   * 若之前绑定了其他会话，先发送 UNBIND 更新绑定（切换页面只更新绑定不断链）；
   * 连接未就绪时先建立连接，由连接建立后的补发流程完成绑定。
   * @param sessionId 会话 ID
   */
  bindSession(sessionId: string): void {
    const previous = this.boundSessionId;
    this.boundSessionId = sessionId;
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.connect();
      return;
    }
    if (previous && previous !== sessionId) {
      this.send({ type: WS_TYPE_UNBIND, sessionId: previous });
    }
    this.send({ type: WS_TYPE_BIND, sessionId });
  }

  /**
   * 解绑指定会话：发送 UNBIND 消息并清除本地记录（若匹配）。
   * @param sessionId 会话 ID
   */
  unbindSession(sessionId: string): void {
    if (this.boundSessionId === sessionId) {
      this.boundSessionId = null;
    }
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.send({ type: WS_TYPE_UNBIND, sessionId });
    }
  }

  /**
   * 注册消息监听器（收到任何服务端消息时回调）。
   * @param listener 消息监听器
   */
  onMessage(listener: WsMessageListener): void {
    this.listeners.add(listener);
  }

  /**
   * 移除消息监听器。
   * @param listener 消息监听器
   */
  offMessage(listener: WsMessageListener): void {
    this.listeners.delete(listener);
  }

  /** 连接建立：重置重连计数，补发当前绑定的会话绑定消息。 */
  private handleOpen(): void {
    this.reconnectAttempts = 0;
    if (this.boundSessionId) {
      this.send({ type: WS_TYPE_BIND, sessionId: this.boundSessionId });
    }
  }

  /** 收到服务端消息：解析 JSON 后分发给所有监听器（单个监听器异常不影响其他监听器）。 */
  private handleMessage(event: MessageEvent): void {
    let payload: Record<string, unknown>;
    try {
      payload = JSON.parse(String(event.data)) as Record<string, unknown>;
    } catch {
      return;
    }
    this.listeners.forEach((listener) => {
      try {
        listener(payload);
      } catch {
        // 忽略单个监听器异常
      }
    });
  }

  /** 连接关闭：非手动关闭时按指数退避调度重连。 */
  private handleClose(): void {
    this.ws = null;
    if (this.manualClosed) {
      return;
    }
    this.scheduleReconnect();
  }

  /** 调度重连：指数退避（1s、2s、4s……上限 30s），重连成功后补发绑定。 */
  private scheduleReconnect(): void {
    if (this.manualClosed || this.reconnectTimer !== null) {
      return;
    }
    const delay = Math.min(
      RECONNECT_BASE_DELAY * 2 ** this.reconnectAttempts,
      RECONNECT_MAX_DELAY,
    );
    this.reconnectAttempts += 1;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);
  }

  /** 清除重连定时器。 */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /** 发送 JSON 消息（连接未就绪时静默丢弃，由绑定/重连流程补发）。 */
  private send(payload: Record<string, unknown>): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }
    this.ws.send(JSON.stringify(payload));
  }
}

/** 全局 WebSocket 客户端单例。 */
export const webSocketClient = new WebSocketClient();
