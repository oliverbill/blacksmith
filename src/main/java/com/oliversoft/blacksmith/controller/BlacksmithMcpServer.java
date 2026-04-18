package com.oliversoft.blacksmith.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.exception.InputBuilderException;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.TenantMCPSummary;
import com.oliversoft.blacksmith.model.dto.input.AgentInput;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.entity.RefinementRequest;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.RefinementRequestRepository;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
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

import java.util.ArrayList;

@Component
public class BlacksmithMcpServer {
    
    private static final Logger log = LoggerFactory.getLogger(BlacksmithMcpServer.class);
    
    private final BlacksmithAgent agent;
    private final JobLauncher jobLauncher;
    private final Job pipelineJob;
    private final TenantRepository tenantRepo;
    private final TenantRunRepository runRepo;
    private final RunArtifactRepository artifactRepo;
    private final ObjectMapper jsonMapper;
    private final InputBuilderRegistry registry;
    private final RefinementRequestRepository refinementRepo;
    
    public BlacksmithMcpServer(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, Job pipelineJob,
           TenantRepository tenantRepo, TenantRunRepository runRepo, ObjectMapper jsonMapper,
           RunArtifactRepository artifactRepo,BlacksmithAgent agent,InputBuilderRegistry registry,
           RefinementRequestRepository refinementRepo)
    {
        this.agent = agent;
        this.jobLauncher = jobLauncher;
        this.pipelineJob = pipelineJob;
        this.tenantRepo = tenantRepo;
        this.runRepo = runRepo;
        this.jsonMapper = jsonMapper;
        this.artifactRepo = artifactRepo;
        this.registry = registry;
        this.refinementRepo = refinementRepo;
    } 

    @Tool(description = "Creates a new Blacksmith pipeline run for a tenant. Use this when the user wants to start analysing a codebase or implement a new feature, bug fix or tech debt. The user might want to skip some step and to start by a specific startStep")
    public String createRun(
        Long tenantId,
        String title,
        String spec,
        IssueType type,
        boolean fullSyncRepo,
        String startStep){
        
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
                .addString("startStep", startStep != null ? startStep : "CONSTITUTION")
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

    @Tool(description = "Requests a refinement of an artifact. Returns the changed artifact for user confirmation.")
    public String refineArtifact(Long artifactId, String feedback) {

        var artifact = artifactRepo.findById(artifactId).orElse(null);
        if (artifact==null) return "Artifact not found :"+artifactId;
        
        var tenant = artifact.getRun().getTenant();
        var agentName = resolveAgentName(artifact.getArtifactType());
        var outputClass = resolveOutputClass(agentName);
        var startStep = resolveStartStep(agentName);
        
        AgentInput input;
        try {
            input = registry.get(agentName).buildInput(tenant, null, feedback);
        } catch (InputBuilderException e) {
            return "Error in the input building: " + e.getMessage();
        }
        
        var output = this.agent.processInput(input, agentName, outputClass);
        String jsonOutput;
        try {
            jsonOutput = this.jsonMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            return "error writing the refinementResult: %s".formatted(e.getMessage());
        }

        var refinement = RefinementRequest.builder()
                            .feedback(feedback)
                            .sourceArtifact(artifact)
                            .tenant(tenant)
                            .startStep(startStep)
                            .refinementResult(jsonOutput)
                        .build();
        
        refinementRepo.save(refinement);

        return jsonOutput;
    }

    @Tool(description = "Confirms a pending refinement request and starts a new run.")
    public String confirmRefinement(Long refinementId) {
        return "nao implementado";
    }

    private String resolveStartStep(AgentName agentName) {
        return switch(agentName){
            case CONSTITUTION -> "CONSTITUTION";
            case ARCHITECT ->  "ARCHITECT";
            case DEVELOPER -> "DEVELOPER";
        };
    }

    private AgentName resolveAgentName(ArtifactType type){
        return switch (type) {
            case CONSTITUTION -> AgentName.CONSTITUTION;
            case IMPACT_ANALYSIS -> AgentName.ARCHITECT;
            case CODE -> AgentName.DEVELOPER;
        };
    }

    private Class<? extends AgentOutput> resolveOutputClass(AgentName agent){
        return switch(agent){
            case CONSTITUTION -> ConstitutionOutput.class;
            case ARCHITECT -> ArchitectOutput.class;
            case DEVELOPER -> DeveloperOutput.class;
        };   
    }

}
