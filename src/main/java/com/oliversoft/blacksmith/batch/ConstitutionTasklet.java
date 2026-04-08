package com.oliversoft.blacksmith.batch;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlackSmithAgent;
import com.oliversoft.blacksmith.core.GitCloner;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.input.ConstitutionInput;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;

@Component
public class ConstitutionTasklet extends AbstractAgentTasklet{

    private static final Logger log = LoggerFactory.getLogger(ConstitutionTasklet.class);
    
    private final GitCloner gitCloner;

    public ConstitutionTasklet(BlackSmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository, 
            ObjectMapper jsonMapper, GitCloner gitCloner) {
        super(agent, runRepository, artifactRepository, taskRepository, jsonMapper);
        this.gitCloner = gitCloner;
    }

    @Override
    protected AgentInput buildInput(TenantRun run) {

        List<String> resolvedPaths = run.getTenant().getGitReposUrls().stream()
            .map(repoUrl -> {
                // Clone repository to local filesystem and return local path
                Path localPath = gitCloner.cloneOrPull(repoUrl);
                return localPath.toString();
            })
            .toList();
        
        var input = new ConstitutionInput(resolvedPaths, run.getTenant().getConstitutionManual());
        return input;
    }

    @Override
    protected Optional<AgentOutput> reuseOutput(TenantRun run) {
        if (run.isFullSyncRepo()) return Optional.empty();
        
        return artifactRepository
            .findTopByRunTenantIdAndAgentNameOrderByCreatedAtDesc(
                run.getTenant().getId(),
                AgentName.CONSTITUTION
            )
            .flatMap(artifact -> {
                try {
                    return Optional.of((AgentOutput) jsonMapper.readValue(artifact.getContent(), ConstitutionOutput.class));
                } catch (Exception e) {
                    log.warn("Failed to deserialize existing constitution, will re-run agent", e);
                    return Optional.empty();
                }
            });
    }

    @Override
    protected AgentName getAgentName() {
        return AgentName.CONSTITUTION;
    }

    @Override
    protected ArtifactType getArtifactType() {
        return ArtifactType.CONSTITUTION;
    }

    @Override
    protected Class<? extends AgentOutput> getOutputType() {
        return ConstitutionOutput.class;
    }
    
}
