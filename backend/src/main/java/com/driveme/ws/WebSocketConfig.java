package com.driveme.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Flutter will connect to ws://<host>:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // tighten later
        // If you later want SockJS for browsers, you can add .withSockJS()
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages sent to /app/** go to @MessageMapping handlers
        registry.setApplicationDestinationPrefixes("/app");

        // Broker sends to subscribers on /topic/** or /queue/**
        registry.enableSimpleBroker("/topic", "/queue");
    }
}
