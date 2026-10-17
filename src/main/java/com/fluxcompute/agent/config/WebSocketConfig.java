package com.fluxcompute.agent.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fluxcompute.agent.service.AgentMessageHandler;



@Configuration 
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
    
    private final AgentMessageHandler agentMessageHandler;
    public WebSocketConfig(AgentMessageHandler agentMessageHandler){
        this.agentMessageHandler = agentMessageHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentMessageHandler, "/ws/agent")
                .setAllowedOriginPatterns("*");
    }
}