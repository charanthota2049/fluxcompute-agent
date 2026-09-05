package com.fluxcompute.agent.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import oshi.SystemInfo;

@Configuration 
public class OshiConfig {
    @Bean 
    public SystemInfo getSystemInfo(){
        return new SystemInfo();
    }
}
