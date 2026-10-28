
package com.fluxcompute.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class JobResultUploadService {

    private final RestClient restClient;

    public JobResultUploadService(
            @Value("${fluxcompute.server.base-url}") String serverBaseUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(serverBaseUrl)
                .build();
    }

    public void uploadResult(
            Long jobId,
            String status,
            long exitCode,
            String logs
    ) throws Exception {

        // 1. Locate the directory containing the generated artifacts
        Path jobDirectory = Path.of(
                "agent-workloads",
                "jobs",
                jobId.toString()
        ).toAbsolutePath().normalize();

        Path outputDirectory = jobDirectory.resolve("output");

        if (!Files.isDirectory(outputDirectory)) {
            throw new IOException(
                    "Output directory does not exist: " + outputDirectory
            );
        }

        // 2. Create a ZIP file outside the output directory
        Path artifactsZip = jobDirectory.resolve("artifacts.zip");

        createZip(outputDirectory, artifactsZip);

        System.out.println("Artifacts ZIP created: " + artifactsZip);
        System.out.println("ZIP size: " + Files.size(artifactsZip) + " bytes");

        // 3. Read the ZIP file, not the output directory
        byte[] artifactBytes = Files.readAllBytes(artifactsZip);

        // 4. Prepare multipart form data
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("status", status);
        body.add("exitCode", Long.toString(exitCode));
        body.add("logs", logs == null ? "" : logs);

        ByteArrayResource artifactResource =
                new ByteArrayResource(artifactBytes) {
                    @Override
                    public String getFilename() {
                        return "artifacts.zip";
                    }
                };

        HttpHeaders artifactHeaders = new HttpHeaders();
        artifactHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        body.add(
                "artifacts",
                new HttpEntity<>(artifactResource, artifactHeaders)
        );

        // 5. Upload the result to the main FluxCompute server
        restClient.post()
                .uri("/api/jobs/{jobId}/result", jobId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        System.out.println(
                "Result uploaded successfully for job " + jobId
        );
    }

    private void createZip(Path sourceDirectory, Path zipFile)
            throws IOException {

        // Re-create the ZIP each time
        Files.deleteIfExists(zipFile);

        try (OutputStream outputStream =
                     Files.newOutputStream(zipFile);
             ZipOutputStream zipOutputStream =
                     new ZipOutputStream(outputStream)) {

            try (var paths = Files.walk(sourceDirectory)) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                // Preserve paths relative to output/
                                String entryName = sourceDirectory
                                        .relativize(file)
                                        .toString()
                                        .replace('\\', '/');

                                zipOutputStream.putNextEntry(
                                        new ZipEntry(entryName)
                                );

                                Files.copy(file, zipOutputStream);
                                zipOutputStream.closeEntry();

                            } catch (IOException e) {
                                throw new java.io.UncheckedIOException(e);
                            }
                        });
            } catch (java.io.UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }
}
