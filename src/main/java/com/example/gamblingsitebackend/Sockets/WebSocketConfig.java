package com.example.gamblingsitebackend.Sockets;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // Enables STOMP-based WebSocket messaging
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // Use /topic for broadcasting updates
        config.setApplicationDestinationPrefixes("/app");  // Client sends messages to /app prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/poker-game-websocket")
                .setAllowedOrigins("http://localhost:3000","http://192.168.29.208:3000")  // Allow connections from React frontend
                .withSockJS();  // SockJS for fallback support
    }
}
