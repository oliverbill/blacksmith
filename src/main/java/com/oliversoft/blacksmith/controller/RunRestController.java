package com.oliversoft.blacksmith.controller;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;

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

    public RunRestController(TenantRunRepository runRepo, RunArtifactRepository artifactRepo,
            TaskExecutionRepository taskExecutionRepo, TenantRepository tenantRepo,
            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, Job pipelineJob) {
        this.runRepo = runRepo;
        this.artifactRepo = artifactRepo;
        this.taskExecutionRepo = taskExecutionRepo;
        this.tenantRepo = tenantRepo;
        this.jobLauncher = jobLauncher;
        this.pipelineJob = pipelineJob;
    }

    public record CreateRunRequest(
        Long tenantId,
        String title,
        String spec,
        IssueType issueType,
        boolean fullSyncRepo,
        String startStep
    ) {}

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
}
