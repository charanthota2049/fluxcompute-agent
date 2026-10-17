package com.fluxcompute.agent.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import jakarta.annotation.PostConstruct;

@Component
public class AgentWebSocketClient {

    private final AgentMessageHandler agentMessageHandler;

    public AgentWebSocketClient(
            AgentMessageHandler agentMessageHandler) {
        this.agentMessageHandler = agentMessageHandler;
    }

    @PostConstruct
    public void connect() {
        StandardWebSocketClient client =
                new StandardWebSocketClient();

        client.execute(
                agentMessageHandler,
                "ws://localhost:8080/ws/agent?nodeId=1"
        ).whenComplete((WebSocketSession session, Throwable error) -> {
            if (error != null) {
                System.err.println(
                        "WebSocket connection failed: "
                                + error.getMessage());
            } else {
                System.out.println(
                        "Connected to FluxCompute Server");
            }
        });
    }
}