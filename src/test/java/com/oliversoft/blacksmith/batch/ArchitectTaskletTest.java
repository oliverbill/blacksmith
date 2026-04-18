package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import com.oliversoft.blacksmith.util.BlacksmithUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArchitectTasklet.
 *
 * Tests are in the same package to access protected afterSuccess.
 * Verifies that planned tasks from ArchitectOutput are persisted as TaskExecutions.
 */
class ArchitectTaskletTest {

    private ArchitectTasklet tasklet;
    private TaskExecutionRepository taskRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskExecutionRepository.class);
        objectMapper = new ObjectMapper();

        tasklet = new ArchitectTasklet(
            mock(BlacksmithAgent.class),
            mock(TenantRunRepository.class),
            mock(RunArtifactRepository.class),
            taskRepository,
            objectMapper,
            mock(InputBuilderRegistry.class)
        );
    }

    // ── metadata ──────────────────────────────────────────────────────────────

    @Test
    void getAgentName_returnsArchitect() {
        assertThat(tasklet.getAgentName()).isEqualTo(AgentName.ARCHITECT);
    }

    @Test
    void getArtifactType_returnsImpactAnalysis() {
        assertThat(tasklet.getArtifactType()).isEqualTo(ArtifactType.IMPACT_ANALYSIS);
    }

    // ── afterSuccess ──────────────────────────────────────────────────────────

    @Test
    void afterSuccess_withTwoPlannedTasks_savesTwoTaskExecutions() throws Exception {
        var plannedTasks = List.of(
            new ArchitectOutput.PlannedTask("task-1", "Implement service", "src/MyService.java", List.of()),
            new ArchitectOutput.PlannedTask("task-2", "Write unit tests", "src/MyServiceTest.java", List.of("task-1"))
        );
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(
            plan("Refactor auth"), plannedTasks
        ));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        List<TaskExecution> saved = captor.getValue();
        assertThat(saved).hasSize(2);
    }

    @Test
    void afterSuccess_taskExecutionsReferenceTheArtifact() throws Exception {
        var plannedTasks = List.of(
            new ArchitectOutput.PlannedTask("t1", "Task one", "src/One.java", List.of())
        );
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), plannedTasks));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        captor.getValue().forEach(t -> assertThat(t.getArtifact()).isSameAs(artifact));
    }

    @Test
    void afterSuccess_taskExecutionsHaveNonNullPlannedTaskIds() throws Exception {
        var plannedTasks = List.of(
            new ArchitectOutput.PlannedTask("unique-id-A", "Task A", "src/A.java", List.of()),
            new ArchitectOutput.PlannedTask("unique-id-B", "Task B", "src/B.java", List.of())
        );
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), plannedTasks));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        captor.getValue().forEach(t -> assertThat(t.getPlannedTaskId()).isNotNull());
    }

    @Test
    void afterSuccess_distinctTaskIds_producesDistinctUuids() throws Exception {
        var plannedTasks = List.of(
            new ArchitectOutput.PlannedTask("id-A", "Task A", "src/Same.java", List.of()),
            new ArchitectOutput.PlannedTask("id-B", "Task B", "src/Same.java", List.of())
        );
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), plannedTasks));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        List<TaskExecution> tasks = captor.getValue();
        UUID uuidA = tasks.get(0).getPlannedTaskId();
        UUID uuidB = tasks.get(1).getPlannedTaskId();
        assertThat(uuidA).isNotEqualTo(uuidB);
    }

    @Test
    void afterSuccess_sameTaskId_producesIdenticalUuid() throws Exception {
        // Two runs with the same planned task id should yield the same deterministic UUID
        var sameId = "stable-task-id";
        var plannedTasks = List.of(new ArchitectOutput.PlannedTask(sameId, "Task", "src/T.java", List.of()));
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), plannedTasks));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        UUID derivedUuid = captor.getValue().get(0).getPlannedTaskId();
        UUID expected = UUID.nameUUIDFromBytes(sameId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(derivedUuid).isEqualTo(expected);
    }

    @Test
    void afterSuccess_withEmptyPlannedTasks_doesNotCallSaveAll() throws Exception {
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), List.of()));

        tasklet.afterSuccess(run(), artifact, "minimax");

        verify(taskRepository, never()).saveAll(anyList());
    }

    @Test
    void afterSuccess_withNullTaskIdFallsBackToFilenameAndIndex() throws Exception {
        // When task.id() is null/blank, the UUID is derived from filenamePath + index
        var plannedTasks = List.of(
            new ArchitectOutput.PlannedTask(null, "Task with no id", "src/Fallback.java", List.of())
        );
        RunArtifact artifact = artifactWithJson(new ArchitectOutput(plan("P"), plannedTasks));

        tasklet.afterSuccess(run(), artifact, "minimax");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        // UUID derived from "src/Fallback.java-0"
        UUID expected = UUID.nameUUIDFromBytes("src/Fallback.java-0".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(captor.getValue().get(0).getPlannedTaskId()).isEqualTo(expected);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TenantRun run() {
        return TenantRun.builder()
            .tenant(Tenant.builder().id(1L).name("test-tenant").build())
            .build();
    }

    private ArchitectOutput.ChangeManagementPlan plan(String title) {
        return new ArchitectOutput.ChangeManagementPlan(title, "Detail", List.of(), List.of(), List.of(), List.of());
    }

    private RunArtifact artifactWithJson(ArchitectOutput output) throws Exception {
        String json = objectMapper.writeValueAsString(output);
        RunArtifact artifact = mock(RunArtifact.class);
        when(artifact.getContent()).thenReturn(json);
        return artifact;
    }
}
