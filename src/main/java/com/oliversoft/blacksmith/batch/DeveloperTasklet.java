package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.adapter.GitAdapter;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.inputbuilder.DeveloperInputBuilder;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import com.oliversoft.blacksmith.util.BlacksmithUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@JobScope
public class DeveloperTasklet extends AbstractAgentTasklet{

    private static final Logger log = LoggerFactory.getLogger(DeveloperTasklet.class);

    private final GitAdapter git;
    
    public DeveloperTasklet(BlacksmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository,
            ObjectMapper jsonMapper,InputBuilderRegistry inputBuilderRegistry,GitAdapter git)
    {
        super(agent, runRepository, artifactRepository, taskRepository, jsonMapper, inputBuilderRegistry);
        this.git = git;
    }

    @Override
    protected AgentName getAgentName() {
        return AgentName.DEVELOPER;
    }

    @Override
    protected ArtifactType getArtifactType() {
        return ArtifactType.CODE;
    }

    @Override
    protected Class<? extends AgentOutput> getOutputType() {
        return DeveloperOutput.class;
    }

    @Override
    protected boolean allowsDuplicateArtifacts() {
        return true;
    }

    @Override
    protected void afterSuccess(TenantRun run, RunArtifact artifact, String providerName) {

        DeveloperOutput devOut = (DeveloperOutput) BlacksmithUtils.getJsonOutputByArtifact(artifact, getOutputType());
        var inputBuilder = (DeveloperInputBuilder)inputBuilderRegistry.get(AgentName.DEVELOPER);
        TaskExecution ongoingTask = inputBuilder.getOngoingTaskExecution();

        // Mark as IN_PROGRESS before starting
        ongoingTask.setStatus(TaskStatus.DEV_IN_PROGRESS);
        taskRepository.save(ongoingTask);

        int changedCount = devOut.changedFiles() == null ? 0 : devOut.changedFiles().size();
        int newCount = devOut.newFiles() == null ? 0 : devOut.newFiles().size();
        log.info("DeveloperOutput for task {}: changedFiles={}, newFiles={}", ongoingTask.getPlannedTaskId(), changedCount, newCount);

        List<String> allowedUrls = inputBuilder.getAllowedRepositoryUrls();
        
        try {
            // Collect all files first
            List<DeveloperOutput.GeneratedFile> allFiles = new java.util.ArrayList<>();
            if (devOut.changedFiles() != null) allFiles.addAll(devOut.changedFiles());
            if (devOut.newFiles() != null) allFiles.addAll(devOut.newFiles());
            
            // Write all files atomically - if any fails, rollback all
            List<Path> writtenFiles = new java.util.ArrayList<>();
            writeFilesToLocalRepo(allFiles, allowedUrls, writtenFiles);
            
            // All files written successfully
            ongoingTask.setStatus(TaskStatus.DEV_DONE);
            ongoingTask.setLlmProvider(providerName);
            taskRepository.save(ongoingTask);
            log.info("Task {} marked as DEV_DONE, {} files written", ongoingTask.getPlannedTaskId(), writtenFiles.size());
            
        } catch (PipelineExecutionException e) {
            // Mark as FAILED
            ongoingTask.setStatus(TaskStatus.DEV_FAILED);
            taskRepository.save(ongoingTask);
            log.error("Task {} marked as DEV_FAILED: {}", ongoingTask.getPlannedTaskId(), e.getMessage());
            throw e;
        }
    }

    private void writeFilesToLocalRepo(List<DeveloperOutput.GeneratedFile> files, List<String> allowedUrls, List<Path> writtenFiles) {
        if (files == null || files.isEmpty()) return;

        for (DeveloperOutput.GeneratedFile file : files) {
            if (file.repoUrl() == null || !allowedUrls.contains(file.repoUrl())) {
                throw new PipelineExecutionException(
                    "Rejected file '%s': repoUrl '%s' is not in the tenant's allowed repositories %s"
                        .formatted(file.filePath(), file.repoUrl(), allowedUrls));
            }

            Path repoPath = git.getRepoLocalPath(file.repoUrl());
            Path targetFile = repoPath.resolve(file.filePath());

            // garante que o path está dentro do repositório
            if (!targetFile.normalize().startsWith(repoPath.normalize())) {
                throw new PipelineExecutionException("Security violation: path '%s' is outside repository".formatted(file.filePath()));
            }

            try {
                // cria directórios intermédios se não existirem
                Files.createDirectories(targetFile.getParent());

                // escreve o conteúdo
                Files.writeString(targetFile, file.content(), StandardCharsets.UTF_8);
                writtenFiles.add(targetFile);
                log.info("Written file: {}", targetFile);

            } catch (IOException e) {
                // Rollback: delete all files written so far
                log.error("Failed to write file: {}, rolling back {} previously written files", file.filePath(), writtenFiles.size());
                for (Path written : writtenFiles) {
                    try {
                        Files.deleteIfExists(written);
                        log.info("Rolled back file: {}", written);
                    } catch (IOException rollbackEx) {
                        log.error("Failed to rollback file: {}", written, rollbackEx);
                    }
                }
                throw new PipelineExecutionException("Failed to write file: " + file.filePath(), e);
            }
        }
    }

    @Override
    protected RepeatStatus getRepeatStatus() {
        return RepeatStatus.CONTINUABLE;
    }

}
