package com.fluxcompute.agent.service;

import org.springframework.stereotype.Service;

import com.fluxcompute.agent.model.ResourceInfo;
import java.util.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSFileStore;

@Service
public class ResourceMonitoringService{
    private final SystemInfo systemInfo;

    public ResourceMonitoringService(SystemInfo systemInfo){
        this.systemInfo = systemInfo;
    }

    public ResourceInfo getCurrentResources()throws Exception{
            ResourceInfo info = new ResourceInfo();
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            long[] oldTicks =
                    processor.getSystemCpuLoadTicks();
    
            Thread.sleep(1000);
    
            double cpuUsage =
                    processor.getSystemCpuLoadBetweenTicks(oldTicks)
                            * 100;
    
            info.setCpuUsage(cpuUsage);
    
            GlobalMemory memory =
                    systemInfo.getHardware().getMemory();
    
            long total =
                    memory.getTotal();
    
            long available =
                    memory.getAvailable();
    
            info.setTotalMemory(total);
            info.setAvailableMemory(available);
    
            double memoryUsage =
                    ((double) (total - available) / total) * 100;
    
            info.setMemoryUsage(memoryUsage);

            List<OSFileStore>   fileStores = systemInfo.getOperatingSystem().getFileSystem().getFileStores();

            long totalStorage=0,freeStorage=0;
            for(OSFileStore fileStore:fileStores){
                totalStorage += fileStore.getTotalSpace();
                freeStorage += fileStore.getFreeSpace();
            }

            info.setTotalStorage(totalStorage);
            info.setFreeStorage(freeStorage);

            return info;
    }    
}