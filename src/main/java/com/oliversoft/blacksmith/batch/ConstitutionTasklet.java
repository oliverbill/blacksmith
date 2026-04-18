package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.ReusedArtifact;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConstitutionTasklet extends AbstractAgentTasklet{

    private static final Logger log = LoggerFactory.getLogger(ConstitutionTasklet.class);

    public ConstitutionTasklet(BlacksmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository,
            ObjectMapper jsonMapper,InputBuilderRegistry inputBuilderRegistry) {
        super(agent, runRepository, artifactRepository, taskRepository, jsonMapper, inputBuilderRegistry);
    }

    @Override
    protected Optional<ReusedArtifact> reuseConstitutionOutput(TenantRun run) {
        if (run.isFullSyncRepo()) return Optional.empty();
        
        return artifactRepository
            .findTopByRunTenantIdAndAgentNameOrderByCreatedAtDesc(
                run.getTenant().getId(),
                AgentName.CONSTITUTION
            )
            .flatMap(artifact -> {
                try {
                    ConstitutionOutput output = jsonMapper.readValue(artifact.getContent(), ConstitutionOutput.class);
                    return Optional.of(new ReusedArtifact(output, artifact, null));
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
