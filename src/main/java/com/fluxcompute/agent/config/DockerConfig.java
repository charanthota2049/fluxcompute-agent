package com.fluxcompute.agent.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

@Configuration
public class DockerConfig {

    @Bean
    public DockerClient dockerClient() {

        DefaultDockerClientConfig dockerConfig =
                DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost("npipe:////./pipe/dockerDesktopLinuxEngine")
                        .build();
        System.out.println("Docker host: " + dockerConfig.getDockerHost());

        ApacheDockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(dockerConfig.getDockerHost())
                        .sslConfig(dockerConfig.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .responseTimeout(Duration.ofSeconds(30))
                        .build();

        return DockerClientImpl.getInstance(
                dockerConfig,
                httpClient
        );
    }
}