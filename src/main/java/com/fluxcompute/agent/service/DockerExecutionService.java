
package com.fluxcompute.agent.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class DockerExecutionService {

    private static final String IMAGE = "fluxcompute-python-ml:1.0";
    private static final long MEMORY_BYTES = 512L * 1024 * 1024;
    private static final long CPU_NANOS = 1_000_000_000L;
    private static final long TIMEOUT_MINUTES = 10;
    private static final int MAX_LOG_CHARS = 1_000_000;

    private final DockerClient dockerClient;

    public DockerExecutionService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String test() {
        return "Docker containers: "
                + dockerClient.infoCmd().exec().getContainers();
    }

    public String executeMLWorklad(String workloadPath) {
        String containerId = null;
        boolean timedOut = false;

        try {
            Path workspace = Path.of(workloadPath)
                    .toAbsolutePath()
                    .normalize();

            if (!Files.isDirectory(workspace)) {
                throw new IllegalArgumentException(
                        "Workspace directory does not exist: " + workspace);
            }

            if (!Files.isRegularFile(workspace.resolve("train.py"))) {
                throw new IllegalArgumentException(
                        "Workspace must contain train.py");
            }
            
            Path jobDirectory = workspace.getParent();
            Path outputDir = jobDirectory.resolve("output");

            HostConfig hostConfig = new HostConfig()
                    .withBinds(
                                new Bind(
                                    workspace.toString(),
                                    new Volume("/workspace"),
                                    AccessMode.ro
                                ),
                                new Bind(
                                    outputDir.toString(),
                                    new Volume("/output"),
                                    AccessMode.rw
                                )
                            )
                    .withNetworkMode("none")
                    .withReadonlyRootfs(true)
                    .withCapDrop(Capability.ALL)
                    .withSecurityOpts(List.of("no-new-privileges:true"))
                    .withMemory(MEMORY_BYTES)
                    .withMemorySwap(MEMORY_BYTES)
                    .withNanoCPUs(CPU_NANOS)
                    .withPidsLimit(128L)
                    .withTmpFs(Map.of(
                            "/tmp", "rw,noexec,nosuid,size=64m"));

            CreateContainerResponse container = dockerClient
                    .createContainerCmd(IMAGE)
                    .withUser("1000:1000")
                    .withWorkingDir("/workspace")
                    .withReadonlyRootfs(true)
                    .withHostConfig(hostConfig)
                    .withEnv(
                            "PIP_NO_CACHE_DIR=1",
                            "PYTHONDONTWRITEBYTECODE=1")
                    .withCmd("python", "/workspace/train.py")
                    .exec();

            containerId = container.getId();
            System.out.println("Sandbox container created: " + containerId);

            dockerClient.startContainerCmd(containerId).exec();

            final String runningContainerId = containerId;

            CompletableFuture<Integer> waitFuture =
                    CompletableFuture.supplyAsync(() ->
                            dockerClient.waitContainerCmd(runningContainerId)
                                    .start()
                                    .awaitStatusCode());

            int exitCode;

            try {
                exitCode = waitFuture.get(
                        TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                timedOut = true;

                try {
                    dockerClient.stopContainerCmd(containerId)
                            .withTimeout(5)
                            .exec();
                } catch (Exception stopError) {
                    System.err.println(
                            "Could not stop timed-out container: "
                                    + stopError.getMessage());
                }

                throw new RuntimeException(
                        "Workload exceeded its execution timeout", e);
            }

            String logs = getContainerLogs(containerId);

            System.out.println("Sandbox exit code: " + exitCode);
            System.out.println(logs);

            if (exitCode != 0) {
                throw new RuntimeException(
                        "Workload failed with exit code "
                                + exitCode + ". Logs: " + logs);
            }

            return logs;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Workload execution interrupted", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Sandbox execution failed: " + e.getMessage(), e);
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .exec();
                } catch (Exception cleanupError) {
                    System.err.println(
                            "Container cleanup failed: "
                                    + cleanupError.getMessage());
                }
            }
        }
    }

    private String getContainerLogs(String containerId) {
        StringBuilder logs = new StringBuilder();

        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            byte[] payload = frame.getPayload();

                            int remaining = MAX_LOG_CHARS - logs.length();
                            if (remaining > 0) {
                                logs.append(new String(
                                        payload, StandardCharsets.UTF_8),
                                        0,
                                        Math.min(
                                                remaining,
                                                new String(
                                                        payload,
                                                        StandardCharsets.UTF_8)
                                                        .length()));
                            }
                        }
                    })
                    .awaitCompletion();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading logs", e);
        }

        return logs.toString();
    }
}
