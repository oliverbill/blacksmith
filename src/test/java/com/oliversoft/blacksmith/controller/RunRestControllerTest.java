package com.oliversoft.blacksmith.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.model.enumeration.IssueType;
import com.oliversoft.blacksmith.model.enumeration.RunStatus;
import com.oliversoft.blacksmith.persistence.RefinementRequestRepository;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import com.oliversoft.blacksmith.service.RetryService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RunRestController.class)
class RunRestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TenantRunRepository runRepo;
    @MockBean RunArtifactRepository artifactRepo;
    @MockBean TaskExecutionRepository taskExecutionRepo;
    @MockBean TenantRepository tenantRepo;
    @MockBean(name = "asyncJobLauncher") JobLauncher jobLauncher;
    @MockBean(name = "pipelineJob") Job pipelineJob;
    @MockBean RefinementRequestRepository refinementRepo;
    @MockBean BlacksmithAgent agent;
    @MockBean InputBuilderRegistry registry;
    @MockBean JobOperator jobOperator;
    @MockBean JobExplorer jobExplorer;
    @MockBean RetryService retryService;

    // ── GET /api/tenants ──────────────────────────────────────────────────────

    @Test
    void getAllTenants_withNoTenants_returnsEmptyArray() throws Exception {
        when(tenantRepo.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/tenants"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getAllTenants_withTenants_returnsTenantList() throws Exception {
        var tenant = Tenant.builder().id(1L).name("Tenant Alpha").build();
        when(tenantRepo.findAll()).thenReturn(List.of(tenant));

        mockMvc.perform(get("/api/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Tenant Alpha"))
            .andExpect(jsonPath("$[0].id").value(1));
    }

    // ── POST /api/tenant ──────────────────────────────────────────────────────

    @Test
    void createTenant_withValidName_returns200AndSavedTenant() throws Exception {
        var saved = Tenant.builder().id(10L).name("New Tenant").build();
        when(tenantRepo.save(any())).thenReturn(saved);

        var req = new RunRestController.CreateTenantRequest("New Tenant", null, null, List.of());
        mockMvc.perform(post("/api/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("New Tenant"));
    }

    @Test
    void createTenant_withBlankName_returns400() throws Exception {
        var req = new RunRestController.CreateTenantRequest("", null, null, List.of());

        mockMvc.perform(post("/api/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());

        verify(tenantRepo, never()).save(any());
    }

    @Test
    void createTenant_withNullName_returns400() throws Exception {
        var req = new RunRestController.CreateTenantRequest(null, null, null, null);

        mockMvc.perform(post("/api/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    // ── GET /api/runs ─────────────────────────────────────────────────────────

    @Test
    void getAllRuns_withNoRuns_returnsEmptyArray() throws Exception {
        when(runRepo.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/runs"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getAllRuns_withRuns_returnsRunList() throws Exception {
        var tenant = Tenant.builder().id(1L).name("T").build();
        var run = TenantRun.builder().id(5L).tenant(tenant).title("Feature Run")
            .spec("spec").issueType(IssueType.FEATURE).build();
        when(runRepo.findAll()).thenReturn(List.of(run));

        mockMvc.perform(get("/api/runs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Feature Run"))
            .andExpect(jsonPath("$[0].id").value(5));
    }

    // ── GET /api/runs/{id} ────────────────────────────────────────────────────

    @Test
    void getRunById_withValidId_returnsRun() throws Exception {
        var tenant = Tenant.builder().id(1L).name("T").build();
        var run = TenantRun.builder().id(1L).tenant(tenant).title("My Run")
            .spec("spec").issueType(IssueType.FEATURE).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));

        mockMvc.perform(get("/api/runs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("My Run"));
    }

    @Test
    void getRunById_withUnknownId_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/runs/999"))
            .andExpect(status().isNotFound());
    }

    // ── POST /api/runs ────────────────────────────────────────────────────────

    @Test
    void createRun_withValidTenant_returns200AndSavedRun() throws Exception {
        var tenant = Tenant.builder().id(1L).name("T").gitReposUrls(List.of()).build();
        when(tenantRepo.findById(1L)).thenReturn(Optional.of(tenant));

        var saved = TenantRun.builder().id(10L).tenant(tenant).title("New Run")
            .spec("spec").issueType(IssueType.FEATURE).status(RunStatus.STARTED).build();
        when(runRepo.save(any())).thenReturn(saved);

        var req = new RunRestController.CreateRunRequest(1L, "New Run", "spec", IssueType.FEATURE, false, null);
        mockMvc.perform(post("/api/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("New Run"));
    }

    @Test
    void createRun_withUnknownTenant_returns400() throws Exception {
        when(tenantRepo.findById(999L)).thenReturn(Optional.empty());

        var req = new RunRestController.CreateRunRequest(999L, "title", "spec", IssueType.FEATURE, false, null);
        mockMvc.perform(post("/api/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());

        verify(runRepo, never()).save(any());
    }

    // ── POST /api/runs/{id}/stop ──────────────────────────────────────────────

    @Test
    void stopRun_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/runs/999/stop"))
            .andExpect(status().isNotFound());
    }

    @Test
    void stopRun_withNonStartedRun_returns400() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.DONE).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));

        mockMvc.perform(post("/api/runs/1/stop"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void stopRun_withStartedRun_stopsAndReturns200() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.STARTED).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(jobExplorer.findRunningJobExecutions("pipelineJob")).thenReturn(java.util.Set.of());
        when(runRepo.save(any())).thenReturn(run);

        mockMvc.perform(post("/api/runs/1/stop"))
            .andExpect(status().isOk());
    }

    // ── DELETE /api/runs/{id} ─────────────────────────────────────────────────

    @Test
    void deleteRun_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/runs/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteRun_withStartedRun_returns400() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.STARTED).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));

        mockMvc.perform(delete("/api/runs/1"))
            .andExpect(status().isBadRequest());

        verify(runRepo, never()).delete(any());
    }

    @Test
    void deleteRun_withDoneRun_returns200() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.DONE).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(artifactRepo.findByRun(run)).thenReturn(List.of());
        when(taskExecutionRepo.findByArtifactIn(any())).thenReturn(List.of());

        mockMvc.perform(delete("/api/runs/1"))
            .andExpect(status().isOk());

        verify(runRepo).delete(run);
    }

    // ── GET /api/runs/{id}/artifacts ──────────────────────────────────────────

    @Test
    void getArtifactsByRun_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/runs/999/artifacts"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getArtifactsByRun_withValidRun_returnsArtifactList() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));

        var artifact = RunArtifact.builder()
            .id(100L).run(run)
            .agentName(AgentName.CONSTITUTION)
            .artifactType(ArtifactType.CONSTITUTION)
            .content("{}")
            .build();
        when(artifactRepo.findByRun(run)).thenReturn(List.of(artifact));

        mockMvc.perform(get("/api/runs/1/artifacts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(100));
    }

    // ── GET /api/runs/{id}/tasks ──────────────────────────────────────────────

    @Test
    void getTasksByRun_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/runs/999/tasks"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTasksByRun_withValidRun_returnsTaskList() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(taskExecutionRepo.findByArtifactRun(run)).thenReturn(List.of());

        mockMvc.perform(get("/api/runs/1/tasks"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    // ── POST /api/runs/{id}/retry ─────────────────────────────────────────────

    @Test
    void retryRun_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/runs/999/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void retryRun_withNonRetryableRun_returns400() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.CANCELLED).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(retryService.canRetry(run)).thenReturn(false);

        mockMvc.perform(post("/api/runs/1/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void retryRun_withInvalidStartStep_returns400() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.ERROR).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(retryService.canRetry(run)).thenReturn(true);

        var req = new RunRestController.RetryRequest("INVALID_STEP");
        mockMvc.perform(post("/api/runs/1/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void retryRun_withValidStartStep_returns200() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.ERROR).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(retryService.canRetry(run)).thenReturn(true);
        when(runRepo.save(any())).thenReturn(run);

        var req = new RunRestController.RetryRequest("DEVELOPER");
        mockMvc.perform(post("/api/runs/1/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startStep").value("DEVELOPER"));
    }

    // ── GET /api/runs/{id}/retry-info ─────────────────────────────────────────

    @Test
    void getRetryInfo_withUnknownRun_returns404() throws Exception {
        when(runRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/runs/999/retry-info"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getRetryInfo_withNonRetryableRun_returns400() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.CANCELLED).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(retryService.canRetry(run)).thenReturn(false);

        mockMvc.perform(get("/api/runs/1/retry-info"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getRetryInfo_withRetryableRun_returns200WithSuggestedStep() throws Exception {
        var run = TenantRun.builder().id(1L).tenant(tenant()).title("R")
            .spec("s").issueType(IssueType.FEATURE).status(RunStatus.ERROR).build();
        when(runRepo.findById(1L)).thenReturn(Optional.of(run));
        when(retryService.canRetry(run)).thenReturn(true);
        when(retryService.determineStartStep(run)).thenReturn("DEVELOPER");
        when(retryService.getJobExecutionSummary(1L)).thenReturn("summary");

        mockMvc.perform(get("/api/runs/1/retry-info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggestedStartStep").value("DEVELOPER"))
            .andExpect(jsonPath("$.currentStatus").value("ERROR"));
    }

    // ── POST /api/refinements/{id}/reject ─────────────────────────────────────

    @Test
    void rejectRefinement_withUnknownId_returns404() throws Exception {
        when(refinementRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/refinements/999/reject"))
            .andExpect(status().isNotFound());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Tenant tenant() {
        return Tenant.builder().id(1L).name("T").gitReposUrls(List.of()).build();
    }
}
