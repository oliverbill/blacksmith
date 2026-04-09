package com.oliversoft.blacksmith.batch;

import java.util.Optional;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlackSmithAgent;
import com.oliversoft.blacksmith.exception.NoPendingTasksException;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.input.DeveloperInput;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.PlannedTask;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
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

@Component
public class DeveloperTasklet extends AbstractAgentTasklet{

    private TaskExecution ongoingTask;

    public DeveloperTasklet(BlackSmithAgent agent, TenantRunRepository runRepository,
            RunArtifactRepository artifactRepository, TaskExecutionRepository taskRepository, ObjectMapper jsonMapper) {
        super(agent, runRepository, artifactRepository, taskRepository, jsonMapper);
    }

    @Override
    protected AgentInput buildInput(TenantRun run) throws NoPendingTasksException{

        var lastConstitutionFromTenant = artifactRepository.findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(
                                                            run.getTenant().getId(),ArtifactType.CONSTITUTION)
                                                    .orElseThrow(() -> new PipelineExecutionException("Constitution Artifact not found for tenant: " + run.getTenant().getId()));

        ConstitutionOutput constitutionOutputJson = (ConstitutionOutput)super.getJsonOutputByArtifact(lastConstitutionFromTenant, ConstitutionOutput.class);                                                    

        var lastImpactAnalysisFromTenant = artifactRepository.findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(run.getTenant().getId(), ArtifactType.IMPACT_ANALYSIS)
                                                    .orElseThrow(() -> new PipelineExecutionException("Impact Analysis Artifact not found for this run: "+run.getId()));

        ArchitectOutput architectOutput = (ArchitectOutput)super.getJsonOutputByArtifact(lastImpactAnalysisFromTenant, ArchitectOutput.class);

        // get the first PENDING TaskExecution for the artifact => 1 RunArtifact(ArchitectOutput) generates N TaskExecutions
        TaskExecution taskExecution = super.taskRepository.findFirstByArtifactAndStatus(lastImpactAnalysisFromTenant, TaskStatus.DEV_PENDING)
                                                    .orElseThrow(() -> new NoPendingTasksException("TaskExecution not found for this artifact: "+lastImpactAnalysisFromTenant.getId()));

        this.ongoingTask = taskExecution;

        PlannedTask plannedTask = getPlannedTaskByOutputAndTaskExecution(architectOutput, taskExecution);

        return new DeveloperInput(plannedTask, architectOutput, constitutionOutputJson);
    }

    private PlannedTask getPlannedTaskByOutputAndTaskExecution(ArchitectOutput architectOutput, TaskExecution taskExecution) {
        var tasks = architectOutput.plannedTasks();
        for (int i = 0; i < tasks.size(); i++) {
            var t = tasks.get(i);
            String rawId = (t.id() != null && !t.id().isBlank()) ? t.id() : (t.filenamePath() + "-" + i);
            java.util.UUID derived = java.util.UUID.nameUUIDFromBytes(rawId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (derived.equals(taskExecution.getPlannedTaskId())) {
                return t;
            }
        }
        throw new PipelineExecutionException("PlannedTask not found for taskExecution " + taskExecution.getId());
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
    protected void afterSuccess(TenantRun run, RunArtifact artifact) {
        this.ongoingTask.setStatus(TaskStatus.DEV_DONE);
        taskRepository.save(this.ongoingTask);
    }

    @Override
    protected RepeatStatus getRepeatStatus() {
        return RepeatStatus.CONTINUABLE;
    }
    
    protected Optional<AgentOutput> reuseOutput(TenantRun run) {
        return Optional.empty();
    }
}
