package com.oliversoft.blacksmith.service;

import com.oliversoft.blacksmith.batch.RetryPipelineJobConfig;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.model.enumeration.TaskStatus;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RetryService {
    
    private static final Logger log = LoggerFactory.getLogger(RetryService.class);
    
    private final TenantRunRepository runRepository;
    private final RunArtifactRepository artifactRepository;
    private final JobExplorer jobExplorer;
    private final RetryPipelineJobConfig retryConfig;
    private final TaskExecutionRepository taskRepository;

    public RetryService(TenantRunRepository runRepository,
                       RunArtifactRepository artifactRepository, 
                       JobExplorer jobExplorer,
                       RetryPipelineJobConfig retryConfig,
                       TaskExecutionRepository taskRepository) {
        this.runRepository = runRepository;
        this.artifactRepository = artifactRepository;
        this.jobExplorer = jobExplorer;
        this.retryConfig = retryConfig;
        this.taskRepository = taskRepository;
    }

    /**
     * Determines if a run can be retried.
     * A run can be retried if its status is ERROR, STARTED (interrupted), or DONE (completed).
     */
    public boolean canRetry(TenantRun run) {
        return run.getStatus() == RunStatus.ERROR 
            || run.getStatus() == RunStatus.STARTED 
            || run.getStatus() == RunStatus.DONE;
    }

    /**
     * Determines the step from which to start the retry based on the last job execution.
     * Returns the step name (CONSTITUTION, ARCHITECT, or DEVELOPER) from which to start.
     * Returns null if the run has never been executed or is currently running.
     */
    public String determineStartStep(TenantRun run) {
        // First check if there are pending tasks for this run's ArchitectOutput - if so, go to DEVELOPER
        var architectArtifact = artifactRepository.findTopByRunTenantIdAndArtifactTypeOrderByCreatedAtDesc(
                run.getTenant().getId(), ArtifactType.IMPACT_ANALYSIS);
        
        if (architectArtifact.isPresent()) {
            long pendingTaskCount = taskRepository.countByArtifactAndStatus(
                    architectArtifact.get(), TaskStatus.DEV_PENDING);
            if (pendingTaskCount > 0) {
                log.info("Found {} DEV_PENDING tasks for run {}'s ArchitectOutput, suggesting DEVELOPER step", 
                        pendingTaskCount, run.getId());
                return "DEVELOPER";
            }
        }
        
        // Find job executions by runId parameter
        List<JobExecution> executions = findJobExecutionsByRunId(run.getId());
        
        if (executions == null || executions.isEmpty()) {
            log.info("No job executions found for run {}, starting from CONSTITUTION", run.getId());
            return "CONSTITUTION";
        }
        
        // Get the most recent execution
        JobExecution lastExecution = executions.get(executions.size() - 1);
        
        // If the job is still running, don't retry
        if (lastExecution.getStatus() == BatchStatus.STARTED) {
            log.warn("Job for run {} is still STARTED, cannot retry", run.getId());
            return null;
        }
        
        List<String> completedSteps = new ArrayList<>();
        
        for (StepExecution stepExecution : lastExecution.getStepExecutions()) {
            String stepName = stepExecution.getStepName();
            BatchStatus status = stepExecution.getStatus();
            
            // Map step names to agent names based on status
            if (status == BatchStatus.COMPLETED) {
                if (stepName.contains("constitution")) {
                    completedSteps.add("CONSTITUTION");
                } else if (stepName.contains("architect")) {
                    completedSteps.add("ARCHITECT");
                } else if (stepName.contains("developer")) {
                    completedSteps.add("DEVELOPER");
                }
            }
        }
        
        log.info("Completed steps for run {}: {}", run.getId(), completedSteps);
        return RetryPipelineJobConfig.determineStartStep(completedSteps);
    }

    /**
     * Finds job executions by runId parameter.
     */
    private List<JobExecution> findJobExecutionsByRunId(Long runId) {
        List<JobExecution> result = new ArrayList<>();
        
        // Use JobExplorer to find job instances and their executions
        List<org.springframework.batch.core.JobInstance> instances = jobExplorer.findJobInstancesByJobName("pipelineJob", 0, 100);
        for (org.springframework.batch.core.JobInstance instance : instances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
            for (JobExecution exec : executions) {
                Long execRunId = exec.getJobParameters().getLong("runId");
                if (execRunId != null && execRunId.equals(runId)) {
                    result.add(exec);
                }
            }
        }
        
        // Also check retryPipelineJob
        List<org.springframework.batch.core.JobInstance> retryInstances = jobExplorer.findJobInstancesByJobName("retryPipelineJob", 0, 100);
        for (org.springframework.batch.core.JobInstance instance : retryInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
            for (JobExecution exec : executions) {
                Long execRunId = exec.getJobParameters().getLong("runId");
                if (execRunId != null && execRunId.equals(runId)) {
                    result.add(exec);
                }
            }
        }
        
        // Sort by end time (most recent last)
        result.sort((a, b) -> {
            if (a.getEndTime() == null && b.getEndTime() == null) return 0;
            if (a.getEndTime() == null) return 1;
            if (b.getEndTime() == null) return -1;
            return a.getEndTime().compareTo(b.getEndTime());
        });
        
        return result;
    }

    /**
     * Gets the last JobExecution for a run.
     */
    public JobExecution getLastJobExecution(Long runId) {
        List<JobExecution> executions = findJobExecutionsByRunId(runId);
        if (executions == null || executions.isEmpty()) {
            return null;
        }
        return executions.get(executions.size() - 1);
    }

    /**
     * Gets a summary of the last job execution for a run.
     */
    public String getJobExecutionSummary(Long runId) {
        JobExecution lastExec = getLastJobExecution(runId);
        if (lastExec == null) {
            return "No job execution found for run " + runId;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Job Execution Summary for Run ").append(runId).append(":\n");
        sb.append("  Status: ").append(lastExec.getStatus()).append("\n");
        sb.append("  Steps:\n");
        
        for (StepExecution stepExec : lastExec.getStepExecutions()) {
            sb.append("    - ").append(stepExec.getStepName())
              .append(": ").append(stepExec.getStatus());
            if (stepExec.getExitStatus() != null) {
                sb.append(" (").append(stepExec.getExitStatus().getExitCode()).append(")");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
