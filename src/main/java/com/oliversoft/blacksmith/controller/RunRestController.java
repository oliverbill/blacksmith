package com.oliversoft.blacksmith.controller;

import java.util.List;
import java.util.Set;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.entity.RefinementRequest;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RefinementStatus;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.RefinementRequestRepository;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import com.oliversoft.blacksmith.service.RetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RunRestController {

    private final TenantRunRepository runRepo;
    private final RunArtifactRepository artifactRepo;
    private final TaskExecutionRepository taskExecutionRepo;
    private final TenantRepository tenantRepo;
    private final JobLauncher jobLauncher;
    private final Job pipelineJob;
    private final RefinementRequestRepository refinementRepo;
    private final BlacksmithAgent agent;
    private final InputBuilderRegistry registry;
    private final ObjectMapper jsonMapper;
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;
    private final RetryService retryService;

    public RunRestController(TenantRunRepository runRepo, RunArtifactRepository artifactRepo,
            TaskExecutionRepository taskExecutionRepo, TenantRepository tenantRepo,
            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, Job pipelineJob,
            RefinementRequestRepository refinementRepo, BlacksmithAgent agent,
            InputBuilderRegistry registry, ObjectMapper jsonMapper,
            JobOperator jobOperator, JobExplorer jobExplorer,
            RetryService retryService) {
        this.runRepo = runRepo;
        this.artifactRepo = artifactRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.tenantRepo = tenantRepo;
        this.jobLauncher = jobLauncher;
        this.pipelineJob = pipelineJob;
        this.refinementRepo = refinementRepo;
        this.agent = agent;
        this.registry = registry;
        this.jsonMapper = jsonMapper;
        this.jobOperator = jobOperator;
        this.jobExplorer = jobExplorer;
        this.retryService = retryService;
    }

    public record CreateRunRequest(
        Long tenantId,
        String title,
        String spec,
        IssueType issueType,
        boolean fullSyncRepo,
        String startStep
    ) {}

    public record CreateTenantRequest(
        String name,
        String constitutionManual,
        String constitutionAuto,
        List<String> gitReposUrls
    ) {
        public static Tenant toTenant(CreateTenantRequest req){
            if (req.name == null || req.name.isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            return Tenant.builder()
                        .constitutionAuto(req.constitutionAuto != null ? req.constitutionAuto : "")
                        .constitutionManual(req.constitutionManual)
                        .gitReposUrls(req.gitReposUrls != null ? req.gitReposUrls : List.of())
                        .name(req.name)
                    .build();
        }

    }

    @PostMapping("/runs")
    public ResponseEntity<?> createRun(@RequestBody CreateRunRequest req) {
        Tenant tenant = tenantRepo.findById(req.tenantId()).orElse(null);
        if (tenant == null) {
            return ResponseEntity.badRequest().body("Tenant não encontrado: " + req.tenantId());
        }

        TenantRun run = TenantRun.builder()
                .tenant(tenant)
                .title(req.title())
                .spec(req.spec())
                .issueType(req.issueType())
                .fullSyncRepo(req.fullSyncRepo())
                .status(RunStatus.STARTED)
                .build();

        TenantRun saved = runRepo.save(run);

        JobParameters params = new JobParametersBuilder()
                .addLong("runId", saved.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .addString("startStep", req.startStep() != null ? req.startStep() : "CONSTITUTION")
                .toJobParameters();

        try {
            jobLauncher.run(pipelineJob, params);
        } catch (JobExecutionException e) {
            return ResponseEntity.internalServerError().body("Erro ao iniciar job: " + e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantRepo.findAll());
    }

    @PostMapping("/tenant")
    public ResponseEntity<?> createTenant(@RequestBody CreateTenantRequest req){
        try {
            Tenant saved = tenantRepo.save(CreateTenantRequest.toTenant(req));
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/runs")
    public ResponseEntity<List<TenantRun>> getAllRuns() {
        return ResponseEntity.ok(runRepo.findAll());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<TenantRun> getRunById(@PathVariable Long id) {
        return runRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/runs/{id}/stop")
    public ResponseEntity<?> stopRun(@PathVariable Long id) {
        var run = runRepo.findById(id).orElse(null);
        if (run == null) return ResponseEntity.notFound().build();
        if (run.getStatus() != RunStatus.STARTED) {
            return ResponseEntity.badRequest().body("Run nao esta em execucao");
        }
        try {
            Set<Long> execIds = jobExplorer.findRunningJobExecutions("pipelineJob")
                .stream()
                .filter(e -> id.equals(e.getJobParameters().getLong("runId")))
                .map(e -> e.getId())
                .collect(java.util.stream.Collectors.toSet());

            for (Long execId : execIds) {
                jobOperator.stop(execId);
            }

            run.setStatus(RunStatus.CANCELLED);
            runRepo.save(run);
            return ResponseEntity.ok(run);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao parar run: " + e.getMessage());
        }
    }

    public record RetryRequest(String startStep) {}

    @PostMapping("/runs/{id}/retry")
    public ResponseEntity<?> retryRun(@PathVariable Long id, @RequestBody(required = false) RetryRequest req) {
        var run = runRepo.findById(id).orElse(null);
        if (run == null) return ResponseEntity.notFound().build();
        
        if (!retryService.canRetry(run)) {
            return ResponseEntity.badRequest().body("Run nao pode ser retried. Status: " + run.getStatus());
        }

        // Determine the start step - use provided or auto-detect
        String startStep;
        if (req != null && req.startStep() != null && !req.startStep().isBlank()) {
            startStep = req.startStep().toUpperCase();
        } else {
            startStep = retryService.determineStartStep(run);
            if (startStep == null) {
                return ResponseEntity.badRequest().body("Run esta em execucao. Aguarde ou pare a run primeiro.");
            }
        }

        // Validate startStep
        if (!List.of("CONSTITUTION", "ARCHITECT", "DEVELOPER").contains(startStep)) {
            return ResponseEntity.badRequest().body("startStep invalido. Use: CONSTITUTION, ARCHITECT, ou DEVELOPER");
        }

        try {
            run.setStatus(RunStatus.STARTED);
            runRepo.save(run);

            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", run.getId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("startStep", startStep)
                    .toJobParameters();

            jobLauncher.run(pipelineJob, params);
            
            return ResponseEntity.ok(new RetryResponse(run.getId(), startStep, 
                    "Retry initiated from step: " + startStep));
        } catch (Exception e) {
            run.setStatus(RunStatus.ERROR);
            runRepo.save(run);
            return ResponseEntity.internalServerError().body("Erro ao fazer retry: " + e.getMessage());
        }
    }

    public record RetryResponse(Long runId, String startStep, String message) {}

    @GetMapping("/runs/{id}/retry-info")
    public ResponseEntity<?> getRetryInfo(@PathVariable Long id) {
        var run = runRepo.findById(id).orElse(null);
        if (run == null) return ResponseEntity.notFound().build();
        
        if (!retryService.canRetry(run)) {
            return ResponseEntity.badRequest().body("Run nao pode ser retried. Status: " + run.getStatus());
        }

        String suggestedStartStep = retryService.determineStartStep(run);
        String jobSummary = retryService.getJobExecutionSummary(id);
        
        return ResponseEntity.ok(new RetryInfo(
                run.getId(),
                run.getStatus().name(),
                suggestedStartStep,
                jobSummary
        ));
    }

    public record RetryInfo(Long runId, String currentStatus, String suggestedStartStep, String lastExecutionSummary) {}

    @org.springframework.web.bind.annotation.DeleteMapping("/runs/{id}")
    public ResponseEntity<?> deleteRun(@PathVariable Long id) {
        var run = runRepo.findById(id).orElse(null);
        if (run == null) return ResponseEntity.notFound().build();
        if (run.getStatus() == RunStatus.STARTED) {
            return ResponseEntity.badRequest().body("Nao e possivel excluir uma run em execucao. Para primeiro.");
        }
        try {
            var artifacts = artifactRepo.findByRun(run);

            // 1. delete task_executions that reference any artifact of this run
            taskExecutionRepo.deleteAll(taskExecutionRepo.findByArtifactIn(artifacts));

            // 2. clear self-references between artifacts (sourceResusedArtifact FK)
            artifacts.forEach(a -> a.setSourceResusedArtifact(null));
            artifactRepo.saveAll(artifacts);

            // 3. delete artifacts, then the run
            artifactRepo.deleteAll(artifacts);
            runRepo.delete(run);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao excluir run: " + e.getMessage());
        }
    }

    @GetMapping("/runs/{id}/artifacts")
    public ResponseEntity<List<RunArtifact>> getArtifactsByRun(@PathVariable Long id) {
        return runRepo.findById(id)
                .map(run -> ResponseEntity.ok(artifactRepo.findByRun(run)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/runs/{id}/tasks")
    public ResponseEntity<List<TaskExecution>> getTasksByRun(@PathVariable Long id) {
        return runRepo.findById(id)
                .map(run -> ResponseEntity.ok(taskExecutionRepo.findByArtifactRun(run)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Refinement ────────────────────────────────────────────────────────────

    public record RefineRequest(String feedback) {}

    @PostMapping("/artifacts/{artifactId}/refine")
    public ResponseEntity<?> refineArtifact(@PathVariable Long artifactId, @RequestBody RefineRequest req) {
        var artifact = artifactRepo.findById(artifactId).orElse(null);
        if (artifact == null) return ResponseEntity.notFound().build();

        var tenant = artifact.getRun().getTenant();
        var agentName = resolveAgentName(artifact.getArtifactType());
        var outputClass = resolveOutputClass(agentName);
        var startStep = resolveStartStep(agentName);

        com.oliversoft.blacksmith.model.dto.input.AgentInput input;
        try {
            // nao tem run associada ainda pq precisa de aprovação
            input = registry.get(agentName).buildInput(tenant, null, req.feedback());
        } catch (com.oliversoft.blacksmith.exception.InputBuilderException e) {
            return ResponseEntity.internalServerError().body("Error building input: " + e.getMessage());
        }

        var output = agent.processInput(input, agentName, outputClass);
        String jsonOutput;
        try {
            jsonOutput = jsonMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body("Error serializing output: " + e.getMessage());
        }

        var refinement = RefinementRequest.builder()
                .feedback(req.feedback())
                .sourceArtifact(artifact)
                .tenant(tenant)
                .startStep(startStep)
                .refinementResult(jsonOutput)
                .build();

        var saved = refinementRepo.save(refinement);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/refinements/{id}/confirm")
    public ResponseEntity<?> confirmRefinement(@PathVariable Long id) {
        var refinement = refinementRepo.findById(id).orElse(null);
        if (refinement == null) return ResponseEntity.notFound().build();

        var sourceRun = refinement.getSourceArtifact().getRun();
        var tenant = refinement.getTenant();

        var newRun = TenantRun.builder()
                .tenant(tenant)
                .title(sourceRun.getTitle() + " [Refined]")
                .spec(refinement.getFeedback())
                .issueType(sourceRun.getIssueType())
                .fullSyncRepo(false)
                .status(RunStatus.STARTED)
                .build();

        var savedRun = runRepo.save(newRun);

        JobParameters params = new JobParametersBuilder()
                .addLong("runId", savedRun.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .addString("startStep", refinement.getStartStep())
                .toJobParameters();

        try {
            jobLauncher.run(pipelineJob, params);
        } catch (JobExecutionException e) {
            return ResponseEntity.internalServerError().body("Erro ao iniciar job: " + e.getMessage());
        }

        refinement.setStatus(RefinementStatus.CONFIRMED);
        refinementRepo.save(refinement);
        return ResponseEntity.ok(savedRun);
    }

    @PostMapping("/refinements/{id}/reject")
    public ResponseEntity<?> rejectRefinement(@PathVariable Long id) {
        var refinement = refinementRepo.findById(id).orElse(null);
        if (refinement == null) return ResponseEntity.notFound().build();
        refinement.setStatus(RefinementStatus.CANCELLED);
        refinementRepo.save(refinement);
        return ResponseEntity.ok().build();
    }

    private AgentName resolveAgentName(ArtifactType type) {
        return switch (type) {
            case CONSTITUTION -> AgentName.CONSTITUTION;
            case IMPACT_ANALYSIS -> AgentName.ARCHITECT;
            case CODE -> AgentName.DEVELOPER;
        };
    }

    private Class<? extends AgentOutput> resolveOutputClass(AgentName a) {
        return switch (a) {
            case CONSTITUTION -> ConstitutionOutput.class;
            case ARCHITECT -> ArchitectOutput.class;
            case DEVELOPER -> DeveloperOutput.class;
        };
    }

    private String resolveStartStep(AgentName a) {
        return switch (a) {
            case CONSTITUTION -> "CONSTITUTION";
            case ARCHITECT -> "ARCHITECT";
            case DEVELOPER -> "DEVELOPER";
        };
    }
}
