package com.fluxcompute.agent.service;


import com.fluxcompute.agent.AgentApplication;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;

@Service 
public class DockerExecutionService {
    
    private final AgentApplication agentApplication;
    private final DockerClient dockerClient;

    public DockerExecutionService(DockerClient dockerClient, AgentApplication agentApplication){
        this.dockerClient = dockerClient;
        this.agentApplication = agentApplication;
    }

    public String test(){
        var info = dockerClient.infoCmd().exec();
        String s="Docker containers:"+info.getContainers();
        return s;
    }

    public String executeMLWorklad(String workloadpath){
        // String imageName = "python:3.12";

        // try{
        //     dockerClient.inspectImageCmd(imageName)
        //                 .exec();    
        // }
        // catch(NotFoundException e){
        //     System.out.println("image not found. Pulling "+imageName);

        // }
        
        System.out.println("Starting ML workload");
        
        HostConfig hostConfig = new HostConfig();
        System.out.println("Workload path received: " + workloadpath);
        hostConfig.withBinds(new Bind(workloadpath,new Volume("/workspace")));

        CreateContainerResponse container = dockerClient
                                                        .createContainerCmd("python:3.12")
                                                        .withHostConfig(hostConfig)
                                                        .withCmd(
                                                            "sh",
                                                            "-c",
                                                            "pip install -r /workspace/requirements.txt"
                                                            + " && "
                                                            + "python /workspace/train.py"
                                                        )
                                                        .exec();
        String containerId = container.getId();
        System.out.println("Container created "+containerId);
        
        dockerClient.startContainerCmd(containerId).exec();
        
        System.out.println("Container created "+containerId);
        
        var waitResponse = dockerClient
                                    .waitContainerCmd(containerId)
                                    .start()
                                    .awaitStatusCode();
        
        System.out.println("Container finished with exit code "+waitResponse);

        String logs = getContainerLogs(containerId);

        return logs;
    }

    private String getContainerLogs(String containerId){
        StringBuilder logs = new StringBuilder();

        try{
            dockerClient.logContainerCmd(containerId)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(false)
                        .exec(
                            new ResultCallback.Adapter<Frame>(){

                                @Override 
                                public void onNext(Frame frame){
                                    logs.append(
                                        new String(
                                            frame.getPayload(),
                                            StandardCharsets.UTF_8
                                        )
                                    );
                                }
                            }
                        )
                        .awaitCompletion();
                        
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();

            throw new RuntimeException("Interrupted while reading container logs");
        }

        return logs.toString();
    }
}
