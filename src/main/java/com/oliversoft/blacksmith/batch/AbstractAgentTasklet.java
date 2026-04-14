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
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
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
import com.oliversoft.blacksmith.util.BlacksmithUtils;

@Component
public abstract class AbstractAgentTasklet implements Tasklet{

    private static final Logger log = LoggerFactory.getLogger(AbstractAgentTasklet.class);

    protected final BlacksmithAgent agent;
    protected final TenantRunRepository runRepository;
    protected final RunArtifactRepository artifactRepository;
    protected final TaskExecutionRepository taskRepository;
    protected final ObjectMapper jsonMapper;
    protected final InputBuilderRegistry inputBuilderRegistry;
    protected final BlacksmithUtils utils;

    public AbstractAgentTasklet(BlacksmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository,
            ObjectMapper jsonMapper,InputBuilderRegistry inputBuilderRegistry,BlacksmithUtils utils) {
        this.agent = agent;
        this.runRepository = runRepository;
        this.artifactRepository = artifactRepository;
        this.taskRepository = taskRepository;
        this.jsonMapper = jsonMapper;
        this.inputBuilderRegistry = inputBuilderRegistry;
        this.utils = utils;
    }

    /**
     * Execute the workflow step of the ongoing TenantRun:
     * 1.) Skip this step if param startStep > it ("CONSTITUTION", "ARCHITECT", "DEVELOPER")
     * 2.) The corresponding subclass calls the Agent to build the step's input
     * 2.1) If there is a previously generated ConstitutionOutput to the Tenant, reuse it and do not process the Input again
     * 3.) The agent generates the output based on the Input and saves it to the DB
     * 4.) If it is the architectStep, generates the PlannedTasks(TaskExecution) and save them to the DB
     * 4.1) If it is the developerStep, generates Files out of the newFiles and changedFiles and saves them to the local repo folder
     **/
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
            return resolveSkipStep(run, startStep, contribution);
        }

        ReusedArtifact reused = callProcessInputWithRetry(run, 2);
        
        if (reused == null) {
            log.error("Agent {} failed after all retry attempts", getAgentName());
            return RepeatStatus.FINISHED;
        }

        if (!isOutputValid(reused.agentOutput())) {
            throw new PipelineExecutionException("Agent " + getAgentName() + " returned invalid output after all attempts");
        }

        String jsonOutput = this.utils.toJSON(reused.agentOutput());

        RunArtifact artifactOutput = RunArtifact.builder()
                                        .run(run)
                                        .agentName(this.getAgentName())
                                        .artifactType(this.getArtifactType())
                                        .content(jsonOutput)
                                        .sourceResusedArtifact(reused.sourceArtifact())
                                        .build();

        artifactRepository.save(artifactOutput);

        afterSuccess(run, artifactOutput);

        return getRepeatStatus();
    }

    private ReusedArtifact resolveConstitutionReuse(TenantRun run){
        ReusedArtifact output = null;
        AgentOutput agentOutput;
        RunArtifact sourceArtifact = null;

        Optional<ReusedArtifact> reusedOutput = reuseConstitutionOutput(run);

        if (reusedOutput.isPresent()) {
            log.info("Reusing existing output for agent {}", getAgentName());
            agentOutput = reusedOutput.get().agentOutput();
            sourceArtifact = reusedOutput.get().sourceArtifact();
            output = new ReusedArtifact(agentOutput,sourceArtifact);
        }
        return output;
    }

    private RepeatStatus resolveSkipStep(TenantRun run, String startStep, StepContribution contribution){

        log.info("Skipping step {} — startStep is {}", getAgentName(), startStep);

        var alreadySaved = artifactRepository.findTopByRunAndArtifactTypeOrderByCreatedAtDesc(run, getArtifactType());
        if (alreadySaved.isPresent()) {
            log.info("Artifact of type {} already exists for run {} — skipping duplicate save", getArtifactType(), run.getId());
            afterSuccess(run, alreadySaved.get());
            contribution.setExitStatus(new ExitStatus("SKIP"));
            return RepeatStatus.FINISHED;
        }
        else
        {
            RunArtifact reusedArtifact = artifactRepository
                .findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(run.getTenant().getId(), getArtifactType())
                .orElseThrow(() -> new PipelineExecutionException(
                    "Cannot skip step " + getAgentName() + ": no previous artifact of type " + getArtifactType() + " found for tenant " + run.getTenant().getId()));

            RunArtifact skippedArtifact = RunArtifact.builder()
                .run(run)
                .agentName(getAgentName())
                .artifactType(getArtifactType())
                .content(reusedArtifact.getContent())
                .sourceResusedArtifact(reusedArtifact)
                .build();
            artifactRepository.save(skippedArtifact);
            log.info("Saved reused artifact {} (source: {}) for run {}", skippedArtifact.getId(), reusedArtifact.getId(), run.getId());

            afterSuccess(run, skippedArtifact);

            contribution.setExitStatus(new ExitStatus("SKIP"));
            return RepeatStatus.FINISHED;
        }
    }

    private boolean shouldExitWithSkipStatus(String startStep) {
        if (startStep == null || startStep.isBlank()) return false;
        
        var order = List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER");
        int startIndex = order.indexOf(startStep.toUpperCase());
        int currentIndex = order.indexOf(getAgentName().name());
        return startIndex > 0 && currentIndex < startIndex;
    }
    
    private ReusedArtifact callProcessInput(TenantRun run){
        AgentInput input;
        ReusedArtifact reused = resolveConstitutionReuse(run);
        if (reused == null){
            try {
                var inputBuilder = inputBuilderRegistry.get(getAgentName());
                input = inputBuilder.buildInput(run.getTenant(), run.getSpec());
                log.info("Input size: {} chars", jsonMapper.writeValueAsString(input).length());
                var output = agent.processInput(input, getAgentName(), getOutputType());
                return new ReusedArtifact(output, null); // input built without artifact reuse
            } 
            catch (JsonProcessingException e) {
                throw new PipelineExecutionException("jsonOutput is empty for agent "+this.getAgentName());
            } catch (InputBuilderException e) {
                throw new PipelineExecutionException(e.getMessage());
            }
        }
        else{
            return reused;
        }
    }
    
    /**
     * Calls callProcessInput up to maxAttempts times, catching exceptions from the agent.
     * The agent itself tries multiple providers, so each attempt gives another chance
     * for a different provider to succeed.
     */
    private ReusedArtifact callProcessInputWithRetry(TenantRun run, int maxAttempts) {
        PipelineExecutionException lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Agent {} callProcessInputWithRetry attempt {}/{}", getAgentName(), attempt, maxAttempts);
                ReusedArtifact result = callProcessInput(run);
                
                if (result != null && isOutputValid(result.agentOutput())) {
                    log.info("Agent {} succeeded on attempt {}", getAgentName(), attempt);
                    return result;
                }
                
                if (result != null && attempt < maxAttempts) {
                    log.warn("Agent {} returned invalid output on attempt {}, retrying...", getAgentName(), attempt);
                } else if (result != null) {
                    log.error("Agent {} returned invalid output on final attempt", getAgentName());
                }
                
            } catch (PipelineExecutionException e) {
                lastException = e;
                log.warn("Agent {} failed on attempt {}/{}: {}", getAgentName(), attempt, maxAttempts, e.getMessage());
                // Always try all attempts - the agent already tried all providers internally
            }
        }
        
        if (lastException != null) {
            log.error("Agent {} failed after {} attempts, last error: {}", 
                    getAgentName(), maxAttempts, lastException.getMessage());
        }
        return null;
    }

    protected abstract AgentName getAgentName();
    protected abstract ArtifactType getArtifactType();
    protected abstract Class<? extends AgentOutput> getOutputType();    
    
    protected RepeatStatus getRepeatStatus() {
        return RepeatStatus.FINISHED;
    }

    protected Optional<ReusedArtifact> reuseConstitutionOutput(TenantRun run) {
        return Optional.empty();
    }

    protected boolean isOutputValid(AgentOutput output) {
        return true;
    }

    protected boolean allowsDuplicateArtifacts() {
        return false;
    }

    protected void afterSuccess(TenantRun run, RunArtifact output) {}

}
