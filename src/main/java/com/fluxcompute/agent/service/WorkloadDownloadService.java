package com.fluxcompute.agent.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.web.client.RestClient;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service 
public class WorkloadDownloadService {
    private final RestClient restClient;

    public WorkloadDownloadService(@Value("${fluxcompute.server.base-url}") String serverBaseUrl){
        this.restClient = RestClient.builder()
                            .baseUrl(serverBaseUrl)
                            .build();
    }

    public Path downloadWorkload(Long workloadId,Long jobId)throws Exception{
        
        byte[] zipBytes = restClient.get()
                            .uri("/api/workloads/{id}/download",workloadId)
                            .accept(MediaType.APPLICATION_OCTET_STREAM)
                            .retrieve()
                            .body(byte[].class);
        if(zipBytes==null||zipBytes.length==0){
            throw new IOException("Download workload is empty "+workloadId);
        }

        Path jobDirectory = Path.of(
            "agent-workloads",
            "jobs",
            jobId.toString()
        ).toAbsolutePath().normalize();

        Files.createDirectories(jobDirectory);
        Path zipPath = jobDirectory.resolve("workload.zip");
        Files.write(zipPath, zipBytes);
        System.out.println("Workload downloaded: " + zipPath);
        System.out.println("ZIP size: " + zipBytes.length + " bytes");
        return zipPath;
    }

    public Path extractWorkload(Path zipPath,Long jobId) throws Exception{
        Path jobDirectory = Path.of("agent-workloads"
                                    ,"jobs",
                                    jobId.toString()
                                    )
                                    .toAbsolutePath()
                                    .normalize();

        Path workspace = jobDirectory.resolve("workspace").normalize();
        Path outputDir = jobDirectory.resolve("output").normalize();

        Files.createDirectories(jobDirectory);
        
        Files.createDirectories(workspace);
        Files.createDirectories(outputDir);

        try(InputStream input = Files.newInputStream(zipPath);
            ZipInputStream zip = new ZipInputStream(input)){

                ZipEntry entry;         
                while((entry=zip.getNextEntry())!=null){
                    Path target = workspace.resolve(entry.getName()).normalize();
                    if(!target.startsWith(workspace) || target.equals(workspace)){
                        throw new IOException("unsafe ZIP entry :"+entry.getName());
                    }
                    if(entry.isDirectory()){
                        Files.createDirectories(target);
                    }
                    else{
                        Files.createDirectories(target.getParent());
                        Files.copy(zip,
                                    target,
                                    StandardCopyOption.REPLACE_EXISTING);
                        
                    }
                    zip.closeEntry();
                }
        }

        // Path outputDir = destination.resolve("output");
        // Files.createDirectories(outputDir);
        // try{
        //     Files.setPosixFilePermissions(
        //         outputDir,
        //         java.nio.file.attribute.PosixFilePermissions
        //                 .fromString("rwxr-xr-x")
        //     );
        // }
        // catch(UnsupportedOperationException e){
        //     throw new IOException(
        //         "POSIX permissions are not supported for " + outputDir, e
        //     );
        // }
        return workspace;
    }

}
