/**
 * WebSocket 消息分发处理。
 *
 * <p>订阅全局 WebSocket 客户端的服务端推送，按 messageName 分发：
 * 收到 SEND_USER_MESSAGE 消息后，根据 mainSessionId/parentSessionId/sessionId
 * 定位对应会话页面（当前仅支持继续会话页面 AgentChat）：对应子会话处于激活视图时
 * 调用对话接口（content 传与后端约定的特殊标记）流式展示回复，否则刷新子会话列表；
 * 找不到对应会话页面时触发子会话列表变更事件（供会话列表页刷新）。</p>
 */

import {
  webSocketClient,
  WS_MESSAGE_NAME_SEND_USER_MESSAGE,
} from './websocket';

/** 与后端约定的特殊标记：作为 content 传给对话接口请求数据流（对应 ChatService.SEND_USER_MESSAGE_MARKER）。 */
export const SEND_USER_MESSAGE_MARKER = '[send_user_message]';

/**
 * 服务端推送的 SEND_USER_MESSAGE 消息负载（与后端 SendUserMessage 序列化结构一致）。
 */
export interface SendUserMessagePayload {
  messageName: string;
  sessionId: string;
  conversationId?: string;
  parentSessionId?: string;
  mainSessionId?: string;
  content: string;
}

/** 子会话列表变更监听器（找不到对应会话页面时触发刷新）。 */
export type ChildSessionsChangedListener = (payload: SendUserMessagePayload) => void;

/**
 * 会话页面处理器：由会话页面（AgentChat）注册，供消息分发定位对应页面。
 */
export interface SessionPageHandler {
  /** 页面所属主会话 ID（用于匹配消息的 mainSessionId/parentSessionId/sessionId）。 */
  mainSessionId: string;
  /** 判断指定子会话是否为当前激活视图。 */
  isChildActive: (childSessionId: string) => boolean;
  /** 在子会话页面以特殊标记调用对话接口，流式展示回复。 */
  streamChildReply: (message: SendUserMessagePayload) => void;
  /** 刷新子会话列表。 */
  refreshChildSessions: () => void;
}

/** 已注册的会话页面处理器（按主会话 ID 索引）。 */
const sessionPages = new Map<string, SessionPageHandler>();

/** 子会话列表变更监听器集合。 */
const childSessionsChangedListeners = new Set<ChildSessionsChangedListener>();

/**
 * 注册会话页面处理器（按主会话 ID 索引，重复注册覆盖）。
 * @param handler 页面处理器
 */
export function registerSessionPage(handler: SessionPageHandler): void {
  sessionPages.set(handler.mainSessionId, handler);
}

/**
 * 注销会话页面处理器。
 * @param mainSessionId 主会话 ID
 */
export function unregisterSessionPage(mainSessionId: string): void {
  sessionPages.delete(mainSessionId);
}

/**
 * 订阅子会话列表变更事件（找不到对应会话页面时触发，用于刷新会话列表页）。
 * @param listener 监听器
 */
export function subscribeChildSessionsChanged(listener: ChildSessionsChangedListener): void {
  childSessionsChangedListeners.add(listener);
}

/**
 * 取消订阅子会话列表变更事件。
 * @param listener 监听器
 */
export function unsubscribeChildSessionsChanged(listener: ChildSessionsChangedListener): void {
  childSessionsChangedListeners.delete(listener);
}

/**
 * 判断消息是否归属于指定主会话页面（mainSessionId/parentSessionId/sessionId 任一匹配）。
 * @param handler 页面处理器
 * @param message 消息负载
 * @returns 是否归属该页面
 */
function matchesPage(handler: SessionPageHandler, message: SendUserMessagePayload): boolean {
  return (
    handler.mainSessionId === message.mainSessionId ||
    handler.mainSessionId === message.parentSessionId ||
    handler.mainSessionId === message.sessionId
  );
}

/**
 * 分发 SEND_USER_MESSAGE 消息：
 * - 定位到对应会话页面（当前支持继续会话页面 AgentChat）：
 *   - 对应子会话处于激活视图时，以特殊标记调用对话接口流式展示回复；
 *   - 否则刷新子会话列表；
 * - 找不到对应会话页面时触发子会话列表变更事件（会话列表页刷新）。
 * @param message 消息负载
 */
export function handleSendUserMessage(message: SendUserMessagePayload): void {
  let matched: SessionPageHandler | null = null;
  for (const handler of sessionPages.values()) {
    if (matchesPage(handler, message)) {
      matched = handler;
      break;
    }
  }

  if (!matched) {
    childSessionsChangedListeners.forEach((listener) => {
      try {
        listener(message);
      } catch {
        // 单个监听器异常不影响其他监听器
      }
    });
    return;
  }

  if (message.sessionId && matched.isChildActive(message.sessionId)) {
    matched.streamChildReply(message);
  } else {
    matched.refreshChildSessions();
  }
}

// 订阅全局 WebSocket 客户端消息，按 messageName 分发
webSocketClient.onMessage((payload) => {
  if (payload.messageName === WS_MESSAGE_NAME_SEND_USER_MESSAGE) {
    handleSendUserMessage(payload as unknown as SendUserMessagePayload);
  }
});
