package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class ArchitectTasklet extends AbstractAgentTasklet{

    private static final Logger log = LoggerFactory.getLogger(ArchitectTasklet.class);

    public ArchitectTasklet(BlacksmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository,
            ObjectMapper jsonMapper,InputBuilderRegistry inputBuilderRegistry) {
        super(agent, runRepository, artifactRepository, taskRepository, jsonMapper, inputBuilderRegistry);
    }

    @Override
    protected AgentName getAgentName() {

        return AgentName.ARCHITECT;
    }

    @Override
    protected ArtifactType getArtifactType() {
        
        return ArtifactType.IMPACT_ANALYSIS;
    }

    @Override
    protected Class<? extends AgentOutput> getOutputType() {

        return ArchitectOutput.class;
    }

    @Override
    protected void afterSuccess(TenantRun run, RunArtifact artifact, String providerName) {

        ArchitectOutput output = null;
        try {

            output = super.jsonMapper.readValue(artifact.getContent(), ArchitectOutput.class);

        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to read ArchitectOutput json ", e);
        }
        
        if (output.plannedTasks() == null || output.plannedTasks().isEmpty()) {
            log.info("Architect returned no plannedTasks for run {} — nothing to implement, pipeline will finish gracefully.", run.getId());
            return;
        }

        var tasks = new ArrayList<TaskExecution>();
        for (int i = 0; i < output.plannedTasks().size(); i++) {
            var t = output.plannedTasks().get(i);
            String rawId = (t.id() != null && !t.id().isBlank()) ? t.id() : (t.filenamePath() + "-" + i);
            tasks.add(TaskExecution.builder()
                .plannedTaskId(UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)))
                .artifact(artifact)
                .build());
        }

        taskRepository.saveAll(tasks);
    }
}
