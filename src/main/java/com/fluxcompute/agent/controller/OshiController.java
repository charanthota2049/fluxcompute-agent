package com.fluxcompute.agent.controller;

import org.springframework.web.bind.annotation.RestController;

import com.fluxcompute.agent.dto.PathDto;
import com.fluxcompute.agent.model.ResourceInfo;
import com.fluxcompute.agent.service.DockerExecutionService;
import com.fluxcompute.agent.service.ResourceMonitoringService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
public class OshiController {

    private final ResourceMonitoringService resourceMonitoringService;
    private final DockerExecutionService dockerExecutionService;

    public OshiController(ResourceMonitoringService resourceMonitoringService,DockerExecutionService dockerExecutionService){
        this.dockerExecutionService = dockerExecutionService;
        this.resourceMonitoringService = resourceMonitoringService;
    }

    @GetMapping("/")
    public ResponseEntity<ResourceInfo> getCurrentResources() {
        try{
            // dockerExecutionService.test();
            return ResponseEntity.ok(resourceMonitoringService.getCurrentResources());
        }
        catch(Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/docker/test")
    public ResponseEntity<String> getDockerInfo() {
        return ResponseEntity.ok(dockerExecutionService.test());
    }
    
    @GetMapping("/docker/ml")
    public String executeMLWorkload(@RequestBody PathDto workloadPath) {
        System.out.println(workloadPath.getPath());
        return dockerExecutionService.executeMLWorklad(workloadPath.getPath());
    }
    
    
}
