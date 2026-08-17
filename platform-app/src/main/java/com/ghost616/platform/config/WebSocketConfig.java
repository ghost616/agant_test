package com.ghost616.platform.config;

import com.ghost616.platform.websocket.AuthHandshakeInterceptor;
import com.ghost616.platform.websocket.SessionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 服务端配置。
 *
 * <p>注册会话 WebSocket Handler 到 /ws 路径，并挂载握手鉴权拦截器
 * （复用 SESSION_ID Cookie 完成用户鉴权）。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /** WebSocket 端点路径。 */
    public static final String WS_ENDPOINT = "/ws";

    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sessionWebSocketHandler, WS_ENDPOINT)
                .addInterceptors(authHandshakeInterceptor);
    }
}