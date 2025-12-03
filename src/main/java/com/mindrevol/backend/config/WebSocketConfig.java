package com.mindrevol.backend.config;

import com.mindrevol.backend.config.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // --- CẤU HÌNH CHO MÔI TRƯỜNG DEV/MVP (IN-MEMORY) ---
        registry.enableSimpleBroker("/topic", "/queue");
        
        // --- CẤU HÌNH CHO SCALE (RABBITMQ / ACTIVEMQ) ---
        // Khi deploy thật, hãy uncomment đoạn này và comment dòng trên lại.
        /*
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("rabbitmq") // Tên service trong docker-compose
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest");
        */

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); 
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}