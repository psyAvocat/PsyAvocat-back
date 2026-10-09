package com.psyavocat.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Point d'entrée WebSocket unique : {@code /ws?token=<Firebase ID token>}.
 * Utilisé par Flutter (et réutilisable par Angular) pour les événements temps réel.
 */
@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeInterceptor handshakeInterceptor;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    public RealtimeWebSocketConfig(RealtimeWebSocketHandler handler,
                                   RealtimeHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Les applications mobiles n'envoient pas d'en-tête Origin ; les navigateurs
        // sont limités aux motifs d'origine CORS autorisés.
        registry.addHandler(handler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
