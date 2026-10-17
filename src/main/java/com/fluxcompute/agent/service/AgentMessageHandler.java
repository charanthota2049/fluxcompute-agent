package com.fluxcompute.agent.service;

import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Json;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AgentMessageHandler
 */
@Component 
public class AgentMessageHandler extends TextWebSocketHandler {
    private final DockerExecutionService dockerExecutionService;
    private final AgentStatusService agentStatusService;
    private final ObjectMapper objectMapper=new ObjectMapper();

    public AgentMessageHandler(DockerExecutionService dockerExecutionService,AgentStatusService agentStatusService){
        this.dockerExecutionService = dockerExecutionService;
        this.agentStatusService = agentStatusService;
    }

    @Override 
    public void handleTextMessage(WebSocketSession session,TextMessage message)throws Exception{
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText();
        if(type.equals("RUN_JOB")){
            Long workLoadId = json.path("workloadId").asLong();
            Long jobId = json.path("jobId").asLong();
            agentStatusService.setSession(session);
        }
    }
}
