package com.ghost616.platform.websocket;

import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器。
 *
 * <p>握手时从请求 Cookie 中读取 SESSION_ID，复用 {@link UserSessionManager}
 * 完成用户鉴权（与 HTTP 接口鉴权同一套会话体系）。鉴权通过后将
 * {@link UserSession} 放入握手属性，供 {@link SessionWebSocketHandler}
 * 获取当前登录用户；鉴权失败（Cookie 缺失或会话不存在）直接拒绝握手。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    /** 握手属性中存放 UserSession 的键。 */
    public static final String USER_SESSION_ATTR = "userSession";

    private final UserSessionManager userSessionManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String sessionId = resolveSessionId(request);
        UserSession userSession = userSessionManager.getSession(sessionId);
        if (userSession == null) {
            log.warn("WebSocket 握手鉴权失败: 缺少有效 SESSION_ID, 拒绝连接");
            return false;
        }
        attributes.put(USER_SESSION_ATTR, userSession);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需额外处理
    }

    /**
     * 从握手请求 Cookie 头中解析会话 ID。
     *
     * @param request 握手请求
     * @return 会话 ID，Cookie 缺失或未找到会话 Cookie 时返回 null
     */
    private String resolveSessionId(ServerHttpRequest request) {
        List<String> cookieHeaders = request.getHeaders().get(HttpHeaders.COOKIE);
        if (cookieHeaders == null) {
            return null;
        }
        for (String cookieHeader : cookieHeaders) {
            if (cookieHeader == null) {
                continue;
            }
            for (String part : cookieHeader.split(";")) {
                String[] pair = part.trim().split("=", 2);
                if (pair.length == 2 && UserSessionManager.SESSION_COOKIE_NAME.equals(pair[0].trim())) {
                    return pair[1].trim();
                }
            }
        }
        return null;
    }
}