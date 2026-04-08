package com.oliversoft.blacksmith.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PipelineJobConfig {
    
    private final JobRepository repository;
    private final PlatformTransactionManager manager;
    private final ConstitutionTasklet constitutionTasklet;
    private final ArchitectTasklet architectTasklet;
    private final DeveloperTasklet developerTasklet;
    private final PipelineJobExecutionListener pipelineJobExecutionListener;

    public PipelineJobConfig(JobRepository repository,
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
    public Step constitutionStep(){

        return new StepBuilder("constitutionStep", repository)
                    .tasklet(this.constitutionTasklet, this.manager)
                    .build();
    }

    @Bean
    public Step architectStep(){

        return new StepBuilder("architectStep", repository)
                    .tasklet(this.architectTasklet, this.manager)
                    .build();
    }

    @Bean
    public Step developerStep(){

        return new StepBuilder("developerStep", repository)
                    .tasklet(this.developerTasklet, this.manager)
                    .build();
    }

    @Bean
    public Job pipelineJob() {

        return new JobBuilder("pipelineJob", repository)
            .listener(this.pipelineJobExecutionListener)
            .start(constitutionStep())
            .next(architectStep())
            .next(developerStep())
            .build();
    }

    @Bean
    public JobLauncher asyncJobLauncher() throws Exception {
        var launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        launcher.afterPropertiesSet();
        return launcher;
    }

}
