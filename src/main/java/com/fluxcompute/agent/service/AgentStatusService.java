package com.fluxcompute.agent.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jvnet.hk2.annotations.Service;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fluxcompute.agent.model.ResourceInfo;

@Component 
public class AgentStatusService {
    private final ResourceMonitoringService resourceMonitoringService;
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
    private volatile WebSocketSession session;
    private final ObjectMapper objectMapper=new ObjectMapper();

    public AgentStatusService(ResourceMonitoringService resourceMonitoringService){
        this.resourceMonitoringService = resourceMonitoringService;

        scheduler.scheduleAtFixedRate(()->{
            WebSocketSession curr = session;
            if(curr==null || !curr.isOpen()){
                return;
            }
            try{
                synchronized(curr){
                    if(curr.isOpen()){
                        ObjectNode response = objectMapper.createObjectNode();
                        ResourceInfo info = resourceMonitoringService.getCurrentResources();
                        response.put("type","RESOURCE_STATUS");
                        response.put("cpuUsage",info.getCpuUsage());
                        response.put("totalMemory",info.getTotalMemory());
                        response.put("availableMemory",info.getAvailableMemory());
                        response.put("memoryUsage",info.getMemoryUsage());
                        response.put("totalStorage",info.getTotalStorage());
                        response.put("freeStorage",info.getFreeStorage());
                        curr.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    }
                }
            }
            catch(Exception e){
                System.err.println("failed to send resource status"+e.getMessage());
            }
        },0,3,TimeUnit.SECONDS);
    }

    public void setSession(WebSocketSession session){
        this.session = session;
    }

    public void clearSession(WebSocketSession session){
        if(this.session == session){
            this.session = null;
        }
    }
}
