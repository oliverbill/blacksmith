package com.oliversoft.blacksmith.controller;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.model.dto.TenantMCPSummary;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;

@Component
public class BlacksmithMcpServer {
    
    private static final Logger log = LoggerFactory.getLogger(BlacksmithMcpServer.class);
    
    private final JobLauncher jobLauncher;
    private final Job pipelineJob;
    private final TenantRepository tenantRepo;
    private final TenantRunRepository runRepo;
    private final ObjectMapper jsonMapper;
    
    public BlacksmithMcpServer(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, Job pipelineJob, TenantRepository tenantRepo,
            TenantRunRepository runRepo, ObjectMapper jsonMapper) {
        this.jobLauncher = jobLauncher;
        this.pipelineJob = pipelineJob;
        this.tenantRepo = tenantRepo;
        this.runRepo = runRepo;
        this.jsonMapper = jsonMapper;
    } 

    @Tool(description = "Creates a new Blacksmith pipeline run for a tenant. Use this when the user wants to start analysing a codebase or implement a new feature, bug fix or tech debt.")
    public String createRun(
        Long tenantId,
        String title,
        String spec,
        IssueType type,
        boolean fullSyncRepo){
        
        var op = tenantRepo.findById(tenantId);

        if(op.isEmpty()){
            return "No Tenant found for the id: "+tenantId;
        }else{
            Tenant t = op.get();
            
            var run = TenantRun.builder()
                        .fullSyncRepo(fullSyncRepo)
                        .issueType(type)
                        .spec(spec)
                        .status(RunStatus.STARTED)
                        .tenant(t)
                        .title(title)
                    .build();

            var savedRun = runRepo.save(run);

            JobParameters params = new JobParametersBuilder()
                .addLong("runId", savedRun.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            try {
                jobLauncher.run(this.pipelineJob, params);
            } catch (JobExecutionException e) {
                log.error(" Error at Job Start: " + e);
                return "Error at Job Start:" + e.getMessage();
            } 
            return "Job Started successfully!";
        }
    }

    @Tool(description = "Find all tenants. Use this when the user do not know the tenant id.")
    public String findAllTenants(){

        var tenants = tenantRepo.findAll();

        if(tenants.isEmpty()){
            System.out.println("No tenants found ");
            return "No tenants found ";
        }else{
            System.out.println("tenants found ");
            var tenantList = new ArrayList<TenantMCPSummary>();
            tenants.forEach(t -> tenantList.add(new TenantMCPSummary(t.getId(), t.getName())));
            
            try {
                return jsonMapper.writeValueAsString(tenantList);
            } catch (JsonProcessingException e) {
                log.error("Error at Serializing Tenant list: " + e);
                return "Error at Serializing Tenant list:" + e.getMessage();
            }
        }
    } 
}
