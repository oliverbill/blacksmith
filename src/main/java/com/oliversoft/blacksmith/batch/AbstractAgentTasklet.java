package com.oliversoft.blacksmith.batch;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlackSmithAgent;
import com.oliversoft.blacksmith.exception.NoPendingTasksException;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ReusedArtifact;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;

@Component
public abstract class AbstractAgentTasklet implements Tasklet{

    private static final Logger log = LoggerFactory.getLogger(AbstractAgentTasklet.class);

    protected final BlackSmithAgent agent;
    protected final TenantRunRepository runRepository;
    protected final RunArtifactRepository artifactRepository;
    protected final TaskExecutionRepository taskRepository;
    protected final ObjectMapper jsonMapper;

    public AbstractAgentTasklet(BlackSmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository,
            ObjectMapper jsonMapper) {
        this.agent = agent;
        this.runRepository = runRepository;
        this.artifactRepository = artifactRepository;
        this.taskRepository = taskRepository;
        this.jsonMapper = jsonMapper;
    }
    

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Long runId = chunkContext
            .getStepContext()
            .getStepExecution()
            .getJobParameters()
            .getLong("runId");

        if (runId == null) {
            throw new PipelineExecutionException("Missing required job parameter: runId", null);
        }
        
        TenantRun run = runRepository.findById(runId).orElseThrow(() -> new PipelineExecutionException("Run not found: " + runId, null));

        String startStep = chunkContext
            .getStepContext()
            .getStepExecution()
            .getJobParameters()
            .getString("startStep");

        if(shouldExitWithSkipStatus(startStep)){
            log.info("Skipping step {} — startStep is {}", getAgentName(), startStep);
            onSkip(run);
            contribution.setExitStatus(new ExitStatus("SKIP"));
            return RepeatStatus.FINISHED;
        }

        Optional<ReusedArtifact> reusedOutput = reuseOutput(run);
        
        AgentOutput output;
        RunArtifact sourceArtifact = null;

        if (reusedOutput.isPresent()) {
            log.info("Reusing existing output for agent {}", getAgentName());
            output = reusedOutput.get().output();
            sourceArtifact = reusedOutput.get().sourceArtifact();
        } else {
            AgentInput input;
            try {
                input = buildInput(run);
            } catch (NoPendingTasksException e) {
                log.info("No pending tasks for agent {} — finishing step. Reason: {}", getAgentName(), e.getMessage());
                return RepeatStatus.FINISHED;
            }
            log.info("Input size: {} chars", jsonMapper.writeValueAsString(input).length());
            output = agent.processInput(input, getAgentName(), getOutputType());
        }
        
        String jsonOutput = serializeOutput(output);
        if (jsonOutput == null || jsonOutput.isBlank())
            throw new PipelineExecutionException("jsonOutput is empty for agent "+this.getAgentName());

        RunArtifact artifactOutput = RunArtifact.builder()
                                        .run(run)
                                        .agentName(this.getAgentName())
                                        .artifactType(this.getArtifactType())
                                        .content(jsonOutput)
                                        .sourceResusedArtifact(sourceArtifact)
                                        .build();

        artifactRepository.save(artifactOutput);

        afterSuccess(run, artifactOutput);

        return getRepeatStatus();
    }

    private String serializeOutput(AgentOutput output) {
        String outputJson = "";
        try {
            outputJson = jsonMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to write artifact json ", e);
        }
        return outputJson;
    }

    private boolean shouldExitWithSkipStatus(String startStep) {
        if (startStep == null || startStep.isBlank()) return false;
        
        var order = List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER");
        int startIndex = order.indexOf(startStep.toUpperCase());
        int currentIndex = order.indexOf(getAgentName().name());
        return startIndex > 0 && currentIndex < startIndex;
    }
    
    protected abstract AgentInput buildInput(TenantRun run) throws NoPendingTasksException, JsonProcessingException;
    protected abstract AgentName getAgentName();
    protected abstract ArtifactType getArtifactType();
    protected abstract Class<? extends AgentOutput> getOutputType();    
    
    protected RepeatStatus getRepeatStatus() {
        return RepeatStatus.FINISHED;
    }

    protected Optional<ReusedArtifact> reuseOutput(TenantRun run) {
        return Optional.empty();
    }
    protected void onSkip(TenantRun run) {}

    protected void afterSuccess(TenantRun run, RunArtifact output) {}

    protected AgentOutput getJsonOutputByArtifact(RunArtifact artifact, Class<? extends AgentOutput> outputType) {
        AgentOutput agentOutput = null;

        try {
            agentOutput = jsonMapper.readValue(artifact.getContent(), outputType);
        } catch (JsonProcessingException e) {
            throw new PipelineExecutionException("Failed to read impact analysis artifact json ", e);
        }
        return agentOutput;
    }
}
