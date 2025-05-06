package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.simp.config.ChannelRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TaskScheduler taskScheduler;
    
    public WebSocketConfig(@Lazy TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Set up message prefixes
        config.enableSimpleBroker("/topic")
              .setHeartbeatValue(new long[] {10000, 10000})  // 10 seconds heartbeat
              .setTaskScheduler(this.taskScheduler);
              
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)  // 512KB
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);  // 30 seconds
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024)  // 128KB message size limit
                   .setSendBufferSizeLimit(512 * 1024)  // 512KB
                   .setSendTimeLimit(15 * 1000);  // 15 seconds
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                   .corePoolSize(4)
                   .maxPoolSize(10)
                   .queueCapacity(100);
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                   .corePoolSize(4)
                   .maxPoolSize(10)
                   .queueCapacity(100);
    }
}