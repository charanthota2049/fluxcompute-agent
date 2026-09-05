package com.fluxcompute.agent.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString 
public class ResourceInfo {

    private double cpuUsage;

    private long totalMemory;

    private long availableMemory;

    private double memoryUsage;

    private long totalStorage;

    private long freeStorage;

    // private long usedStorage;
}