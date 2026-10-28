
package com.fluxcompute.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.file.Path;

@Component
public class AgentMessageHandler extends TextWebSocketHandler {

    private final DockerExecutionService dockerExecutionService;
    private final AgentStatusService agentStatusService;
    private final WorkloadDownloadService workloadDownloadService;
    private final JobResultUploadService jobResultUploadService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentMessageHandler(
            DockerExecutionService dockerExecutionService,
            AgentStatusService agentStatusService,
            WorkloadDownloadService workloadDownloadService,
            JobResultUploadService jobResultUploadService) {
        this.dockerExecutionService = dockerExecutionService;
        this.agentStatusService = agentStatusService;
        this.workloadDownloadService = workloadDownloadService;
        this.jobResultUploadService = jobResultUploadService;
        // this.objectMapper = objectMapper;
    }

    @Override
    public void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        Long jobId = null;

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.path("type").asText();

            if (!"RUN_JOB".equals(type)) {
                return;
            }

            JsonNode workloadNode = json.path("workloadId");
            JsonNode jobNode = json.path("jobId");

            if (!workloadNode.canConvertToLong()
                    || !jobNode.canConvertToLong()) {
                throw new IllegalArgumentException(
                        "RUN_JOB requires numeric workloadId and jobId");
            }

            long workloadId = workloadNode.asLong();
            jobId = jobNode.asLong();

            if (workloadId <= 0 || jobId <= 0) {
                throw new IllegalArgumentException(
                        "workloadId and jobId must be positive");
            }

            // Register the session before starting work.
            agentStatusService.setSession(session);

            Path zipPath =
                    workloadDownloadService.downloadWorkload(
                            workloadId, jobId);

            Path workspace =
                    workloadDownloadService.extractWorkload(
                            zipPath, jobId);

            String logs =
                    dockerExecutionService.executeMLWorklad(
                            workspace.toString());
            
            System.out.println(
                    "Job " + jobId + " completed successfully.");
            System.out.println(logs);
                
            jobResultUploadService.uploadResult(jobId, "COMPLETED", workloadId, logs);
            // TODO: Report successful job status to the server
            // using the status message/API contract implemented by
            // your backend.

        } catch (Exception e) {
                e.printStackTrace();
            System.err.println(
                    "Job " + jobId + " failed: " + e.getMessage());

            // TODO: Report FAILED status to the server.
            // Do not invent a message format: use the protocol
            // your backend expects.

            // Intentionally do not rethrow the job exception.
            // This prevents ordinary workload failures from
            // propagating out of this WebSocket handler.
        }
    }
}
