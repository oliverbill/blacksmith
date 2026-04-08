package com.oliversoft.blacksmith.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;

@Component
public class PipelineJobExecutionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(PipelineJobExecutionListener.class);

    private final TenantRunRepository runRepository;

    public PipelineJobExecutionListener(TenantRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long runId = jobExecution.getJobParameters().getLong("runId");
        if (runId == null) {
            return;
        }

        try {
            TenantRun run = runRepository.findById(runId).orElse(null);
            if (run == null) {
                return;
            }

            BatchStatus batchStatus = jobExecution.getStatus();
            run.setStatus(batchStatus == BatchStatus.COMPLETED ? RunStatus.DONE : RunStatus.ERROR);
            runRepository.save(run);
        } catch (Exception e) {
            log.error("Failed to update run status for runId={} after job completion — context may have been closed", runId, e);
        }
    }
}
