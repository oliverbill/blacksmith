package com.oliversoft.blacksmith.inputbuilder;

import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.exception.NoPendingTasksException;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.input.DeveloperInput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput.PlannedTask;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.util.BlacksmithUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class DeveloperInputBuilder implements InputBuilderStrategy{

    private static final Logger log = LoggerFactory.getLogger(DeveloperInputBuilder.class);
    protected TaskExecution ongoingDeveloperTask;
    protected List<String> allowedRepositoryUrls;

    private final RunArtifactRepository artifactRepo;
    private final TaskExecutionRepository taskRepo;

    public DeveloperInputBuilder(RunArtifactRepository artifactRepo, TaskExecutionRepository taskRepo) {
        this.artifactRepo = artifactRepo;
        this.taskRepo = taskRepo;
    }


    @Override
    public AgentInput buildInput(Tenant tenant, TenantRun run, String spec) throws InputBuilderException{

        try{
            var lastConstitutionFromTenant = artifactRepo.findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(
                                                                tenant.getId(),ArtifactType.CONSTITUTION)
                                                        .orElseThrow(() -> new InputBuilderException("Constitution Artifact not found for tenant: " + tenant.getName()));

            ConstitutionOutput constitutionOutputJson = (ConstitutionOutput) BlacksmithUtils.getJsonOutputByArtifact(lastConstitutionFromTenant, ConstitutionOutput.class);

            /* 
                Use the artifact linked to the current run; if it is a skipped copy, 
                follow sourceResusedArtifact to find the original where the TaskExecutions actually live.
            */ 
            RunArtifact impactArtifact = artifactRepo
                .findTopByRunAndArtifactTypeOrderByCreatedAtDesc(run, ArtifactType.IMPACT_ANALYSIS)
                .orElseThrow(() -> new InputBuilderException("Impact Analysis Artifact not found for run: " + run.getId()));

            RunArtifact taskOwnerArtifact = impactArtifact.getSourceResusedArtifact() != null
                ? impactArtifact.getSourceResusedArtifact()
                : impactArtifact;

            ArchitectOutput architectOutput = (ArchitectOutput) BlacksmithUtils.getJsonOutputByArtifact(taskOwnerArtifact, ArchitectOutput.class);

            // get the first PENDING TaskExecution for the artifact => 1 RunArtifact(ArchitectOutput) generates N TaskExecutions
            TaskExecution taskExecution = this.taskRepo.findFirstByArtifactAndStatus(taskOwnerArtifact, TaskStatus.DEV_PENDING)
                                                        .orElseThrow(() ->
                                                            new NoPendingTasksException("TaskExecution not found for impact analysis: " + taskOwnerArtifact.getId()));

            this.ongoingDeveloperTask = taskExecution; // save it to retrieve it on afterSuccess()
            this.allowedRepositoryUrls = tenant.getGitReposUrls();

            PlannedTask plannedTask = getPlannedTaskByOutputAndTaskExecution(architectOutput, taskExecution);

            return new DeveloperInput(plannedTask, architectOutput, constitutionOutputJson, tenant.getGitReposUrls(), spec);

        }catch (NoPendingTasksException e) {
            var msg = "No pending tasks for agent %s — finishing step. Reason: %s".formatted(AgentName.DEVELOPER, e.getMessage());
            log.info(msg);
            throw new InputBuilderException(msg);
        }
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

    public TaskExecution getOngoingTaskExecution(){
        return this.ongoingDeveloperTask;
    }

}
