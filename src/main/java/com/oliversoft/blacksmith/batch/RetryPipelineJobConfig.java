package com.oliversoft.blacksmith.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class RetryPipelineJobConfig {
    
    private static final Logger log = LoggerFactory.getLogger(RetryPipelineJobConfig.class);
    
    private final JobRepository repository;
    private final PlatformTransactionManager manager;
    private final ConstitutionTasklet constitutionTasklet;
    private final ArchitectTasklet architectTasklet;
    private final DeveloperTasklet developerTasklet;
    private final PipelineJobExecutionListener pipelineJobExecutionListener;

    public RetryPipelineJobConfig(JobRepository repository,
                             PlatformTransactionManager manager,
                             ConstitutionTasklet constitutionTasklet,
                             ArchitectTasklet architectTasklet,
                             DeveloperTasklet developerTasklet,
                             PipelineJobExecutionListener pipelineJobExecutionListener){

        this.repository = repository;
        this.manager = manager;
        this.constitutionTasklet = constitutionTasklet;
        this.architectTasklet = architectTasklet;      
        this.developerTasklet = developerTasklet;
        this.pipelineJobExecutionListener = pipelineJobExecutionListener;
    }
    
    @Bean
    public Step constitutionRetryStep(){

        return new StepBuilder("constitutionRetryStep", repository)
                    .tasklet(this.constitutionTasklet, this.manager)
                    .build();
    }

    @Bean
    public Step architectRetryStep(){

        return new StepBuilder("architectRetryStep", repository)
                    .tasklet(this.architectTasklet, this.manager)
                    .build();
    }

    @Bean
    public Step developerRetryStep(){

        return new StepBuilder("developerRetryStep", repository)
                    .tasklet(this.developerTasklet, this.manager)
                    .build();
    }

    @Bean
    public Job retryPipelineJob() {

        return new JobBuilder("retryPipelineJob", repository)
            .listener(this.pipelineJobExecutionListener)
            .start(constitutionRetryStep())
                .on("SKIP").to(architectRetryStep())
            
            .from(constitutionRetryStep())
                .on("*").to(architectRetryStep())
            
            .from(architectRetryStep())
                .on("SKIP").to(developerRetryStep())

            .from(architectRetryStep())
                .on("*").to(developerRetryStep())

            .from(developerRetryStep())
                .on("*").end()

            .build()
            .build();
    }

    /**
     * Determines the last successful step from a list of step names in execution order.
     * Returns the step to start from on retry (exclusive - the step AFTER the last successful one).
     */
    public static String determineStartStep(List<String> completedSteps) {
        List<String> stepOrder = List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER");
        
        for (int i = stepOrder.size() - 1; i >= 0; i--) {
            String step = stepOrder.get(i);
            if (completedSteps.contains(step)) {
                if (i < stepOrder.size() - 1) {
                    log.info("Last completed step: {}, retry will start from: {}", step, stepOrder.get(i + 1));
                    return stepOrder.get(i + 1);
                } else {
                    log.info("All steps completed, no retry needed");
                    return null;
                }
            }
        }
        
        log.info("No completed steps found, retry will start from CONSTITUTION");
        return "CONSTITUTION";
    }
}
