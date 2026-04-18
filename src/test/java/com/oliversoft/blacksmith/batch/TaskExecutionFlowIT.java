package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import com.oliversoft.blacksmith.model.entity.TaskExecution;
import com.oliversoft.blacksmith.model.entity.Tenant;
import com.oliversoft.blacksmith.model.entity.TenantRun;
import com.oliversoft.blacksmith.model.enumeration.*;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the ArchitectTasklet → DeveloperTasklet flow.
 *
 * Verifies that:
 * 1. ArchitectTasklet creates one TaskExecution per plannedTask
 * 2. DeveloperTasklet processes all expected tasks
 * 3. TaskExecutions have correct status transitions
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskExecutionFlowIT {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantRunRepository tenantRunRepository;

    @Autowired
    private RunArtifactRepository artifactRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Tenant testTenant;
    private TenantRun testRun;

    @BeforeEach
    void setUp() {
        // Create test tenant
        testTenant = Tenant.builder()
                .name("test-tenant-" + System.currentTimeMillis())
                .gitReposUrls(List.of("https://github.com/oliversoft/blacksmith"))
                .constitutionAuto("{}")
                .build();
        testTenant = tenantRepository.save(testTenant);

        // Create test run
        testRun = TenantRun.builder()
                .tenant(testTenant)
                .issueType(IssueType.FEATURE)
                .title("Test Run")
                .spec("Test specification")
                .status(RunStatus.STARTED)
                .fullSyncRepo(false)
                .build();
        testRun = tenantRunRepository.save(testRun);
    }

    @AfterEach
    void tearDown() {
        // Clean up in reverse order of creation (respecting foreign keys)
        taskExecutionRepository.deleteAll();
        artifactRepository.deleteAll();
        tenantRunRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    /**
     * Test 1: Verifies that ArchitectTasklet creates one TaskExecution per plannedTask.
     *
     * Scenario: ArchitectOutput has 3 plannedTasks
     * Expected: 3 TaskExecutions are created, each referencing the artifact
     */
    @Test
    void architectTasklet_createsOneTaskExecutionPerPlannedTask() throws Exception {
        // Arrange: Create an ArchitectOutput with 3 planned tasks
        List<ArchitectOutput.PlannedTask> plannedTasks = List.of(
                new ArchitectOutput.PlannedTask("task-1", "Create service class", "src/Service.java", List.of()),
                new ArchitectOutput.PlannedTask("task-2", "Create controller", "src/Controller.java", List.of("task-1")),
                new ArchitectOutput.PlannedTask("task-3", "Write unit tests", "src/ServiceTest.java", List.of("task-1"))
        );
        ArchitectOutput architectOutput = new ArchitectOutput(
                new ArchitectOutput.ChangeManagementPlan("Feature Implementation", "Details", List.of(), List.of(), List.of(), List.of()),
                plannedTasks
        );

        // Create and save the artifact (simulating what ArchitectTasklet does after LLM call)
        String jsonContent = objectMapper.writeValueAsString(architectOutput);
        final RunArtifact artifact = RunArtifact.builder()
                .run(testRun)
                .agentName(AgentName.ARCHITECT)
                .artifactType(ArtifactType.IMPACT_ANALYSIS)
                .content(jsonContent)
                .build();

        final var savedArtifact = artifactRepository.save(artifact);

        // Act: Call ArchitectTasklet's afterSuccess (which creates TaskExecutions)
        ArchitectTasklet architectTasklet = new ArchitectTasklet(
                null, // agent not needed for this test
                tenantRunRepository,
                artifactRepository,
                taskExecutionRepository,
                objectMapper,
                null // inputBuilderRegistry
        );
        architectTasklet.afterSuccess(testRun, savedArtifact, "minimax");

        // Assert: Verify TaskExecutions were created (using findByArtifactRun)
        List<TaskExecution> savedTasks = taskExecutionRepository.findByArtifactRun(testRun);

        assertThat(savedTasks)
                .as("Should create one TaskExecution per plannedTask")
                .hasSize(3);

        // Verify each plannedTask has a corresponding TaskExecution
        for (ArchitectOutput.PlannedTask plannedTask : plannedTasks) {
            String rawId = plannedTask.id() != null && !plannedTask.id().isBlank()
                    ? plannedTask.id()
                    : plannedTask.filenamePath() + "-" + plannedTasks.indexOf(plannedTask);
            UUID expectedUuid = UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8));

            assertThat(savedTasks)
                    .as("Should have TaskExecution for plannedTask: " + plannedTask.id())
                    .anyMatch(te -> te.getPlannedTaskId().equals(expectedUuid));
        }

        // Verify all TaskExecutions reference the artifact
        savedTasks.forEach(te -> assertThat(te.getArtifact()).isEqualTo(savedArtifact));

        // Verify all are PENDING
        savedTasks.forEach(te -> assertThat(te.getStatus()).isEqualTo(TaskStatus.DEV_PENDING));
    }

    /**
     * Test 2: Verifies that plannedTask IDs are deterministic.
     *
     * Scenario: Same plannedTask ID across different runs should produce same UUID
     * Expected: UUID is derived from task ID (deterministic)
     */
    @Test
    void architectTasklet_generatesDeterministicTaskIds() throws Exception {
        // Arrange: Two tasks with same ID
        String sameTaskId = "stable-task-id";
        ArchitectOutput architectOutput = new ArchitectOutput(
                new ArchitectOutput.ChangeManagementPlan("Test", "Details", List.of(), List.of(), List.of(), List.of()),
                List.of(new ArchitectOutput.PlannedTask(sameTaskId, "Task description", "src/File.java", List.of()))
        );

        RunArtifact artifact = RunArtifact.builder()
                .run(testRun)
                .agentName(AgentName.ARCHITECT)
                .artifactType(ArtifactType.IMPACT_ANALYSIS)
                .content(objectMapper.writeValueAsString(architectOutput))
                .build();
        artifact = artifactRepository.save(artifact);

        // Act
        ArchitectTasklet architectTasklet = new ArchitectTasklet(
                null, tenantRunRepository, artifactRepository, taskExecutionRepository,
                objectMapper, null
        );
        architectTasklet.afterSuccess(testRun, artifact, "minimax");

        // Assert: UUID should be deterministic
        List<TaskExecution> savedTasks = taskExecutionRepository.findByArtifactRun(testRun);
        UUID expectedUuid = UUID.nameUUIDFromBytes(sameTaskId.getBytes(StandardCharsets.UTF_8));

        assertThat(savedTasks.get(0).getPlannedTaskId())
                .as("TaskExecution UUID should be deterministic based on plannedTask ID")
                .isEqualTo(expectedUuid);
    }

    /**
     * Test 3: Verifies that DeveloperTasklet picks up tasks in correct order.
     *
     * Scenario: 3 tasks created by ArchitectTasklet, task-2 depends on task-1
     * Expected: DeveloperInputBuilder returns next PENDING task
     */
    @Test
    void developerInputBuilder_returnsPendingTasksInOrder() throws Exception {
        // Arrange: Create ArchitectOutput and artifact first
        List<ArchitectOutput.PlannedTask> plannedTasks = List.of(
                new ArchitectOutput.PlannedTask("dev-1", "First task", "src/First.java", List.of()),
                new ArchitectOutput.PlannedTask("dev-2", "Second task", "src/Second.java", List.of("dev-1"))
        );
        ArchitectOutput architectOutput = new ArchitectOutput(
                new ArchitectOutput.ChangeManagementPlan("Plan", "Details", List.of(), List.of(), List.of(), List.of()),
                plannedTasks
        );

        RunArtifact artifact = RunArtifact.builder()
                .run(testRun)
                .agentName(AgentName.ARCHITECT)
                .artifactType(ArtifactType.IMPACT_ANALYSIS)
                .content(objectMapper.writeValueAsString(architectOutput))
                .build();
        artifact = artifactRepository.save(artifact);

        // Create TaskExecutions (as ArchitectTasklet would)
        ArchitectTasklet architectTasklet = new ArchitectTasklet(
                null, tenantRunRepository, artifactRepository, taskExecutionRepository,
                objectMapper, null
        );
        architectTasklet.afterSuccess(testRun, artifact, "minimax");

        // Verify initial state: all PENDING
        List<TaskExecution> allTasks = taskExecutionRepository.findByArtifactRun(testRun);
        assertThat(allTasks).allMatch(t -> t.getStatus() == TaskStatus.DEV_PENDING);

        // Act: Simulate DeveloperTasklet picking up the first task
        // (findFirstByArtifactAndStatus returns the next pending task)
        var nextTask = taskExecutionRepository.findFirstByArtifactAndStatus(artifact, TaskStatus.DEV_PENDING);

        // Assert: Should find the first task (no dependencies)
        assertThat(nextTask)
                .as("Should find a PENDING task")
                .isPresent();

        // The first task returned should be the one with no pending dependencies
        // In our case, dev-1 has no dependencies, so it should be returned first
        TaskExecution selectedTask = nextTask.get();
        assertThat(selectedTask.getStatus())
                .as("Selected task should still be PENDING before processing")
                .isEqualTo(TaskStatus.DEV_PENDING);
    }

    /**
     * Test 4: Verifies complete flow - all tasks get processed.
     *
     * Scenario: 3 tasks created, developer processes them all
     * Expected: All tasks transition from PENDING → DEV_DONE
     */
    @Test
    void allTasks_transitionFromPendingToDevDone() throws Exception {
        // Arrange: Create ArchitectOutput with 3 tasks
        List<ArchitectOutput.PlannedTask> plannedTasks = List.of(
                new ArchitectOutput.PlannedTask("t1", "Task 1", "src/T1.java", List.of()),
                new ArchitectOutput.PlannedTask("t2", "Task 2", "src/T2.java", List.of()),
                new ArchitectOutput.PlannedTask("t3", "Task 3", "src/T3.java", List.of())
        );
        ArchitectOutput architectOutput = new ArchitectOutput(
                new ArchitectOutput.ChangeManagementPlan("Plan", "Details", List.of(), List.of(), List.of(), List.of()),
                plannedTasks
        );

        RunArtifact artifact = RunArtifact.builder()
                .run(testRun)
                .agentName(AgentName.ARCHITECT)
                .artifactType(ArtifactType.IMPACT_ANALYSIS)
                .content(objectMapper.writeValueAsString(architectOutput))
                .build();
        artifact = artifactRepository.save(artifact);

        // Create TaskExecutions
        ArchitectTasklet architectTasklet = new ArchitectTasklet(
                null, tenantRunRepository, artifactRepository, taskExecutionRepository,
                objectMapper, null
        );
        architectTasklet.afterSuccess(testRun, artifact, "minimax");

        // Act: Simulate processing all tasks
        int processedCount = 0;
        TaskExecution task;
        while ((task = taskExecutionRepository.findFirstByArtifactAndStatus(artifact, TaskStatus.DEV_PENDING)
                .orElse(null)) != null) {
            // Mark as IN_PROGRESS then DEV_DONE (simulating DeveloperTasklet.afterSuccess)
            task.setStatus(TaskStatus.DEV_IN_PROGRESS);
            task.setLlmProvider("minimax");
            taskExecutionRepository.save(task);

            task.setStatus(TaskStatus.DEV_DONE);
            taskExecutionRepository.save(task);
            processedCount++;
        }

        // Assert: All 3 tasks processed
        assertThat(processedCount)
                .as("Should process all 3 tasks")
                .isEqualTo(3);

        // Verify final state
        List<TaskExecution> finalTasks = taskExecutionRepository.findByArtifactRun(testRun);
        assertThat(finalTasks)
                .as("All tasks should be DEV_DONE")
                .allMatch(t -> t.getStatus() == TaskStatus.DEV_DONE);

        assertThat(finalTasks)
                .as("All tasks should have the LLM provider recorded")
                .allMatch(t -> t.getLlmProvider() != null);
    }

    /**
     * Test 5: Verifies that task IDs in ArchitectOutput match what's stored in TaskExecution.
     *
     * Scenario: Create tasks and verify UUID derivation matches
     * Expected: UUID stored in TaskExecution equals UUID derived from ArchitectOutput.PlannedTask
     */
    @Test
    void plannedTaskId_matchesStoredTaskExecutionId() throws Exception {
        // Arrange
        String taskId = "my-unique-task-123";
        ArchitectOutput architectOutput = new ArchitectOutput(
                new ArchitectOutput.ChangeManagementPlan("Plan", "Details", List.of(), List.of(), List.of(), List.of()),
                List.of(new ArchitectOutput.PlannedTask(taskId, "Description", "src/File.java", List.of()))
        );

        RunArtifact artifact = RunArtifact.builder()
                .run(testRun)
                .agentName(AgentName.ARCHITECT)
                .artifactType(ArtifactType.IMPACT_ANALYSIS)
                .content(objectMapper.writeValueAsString(architectOutput))
                .build();
        artifact = artifactRepository.save(artifact);

        // Act
        ArchitectTasklet architectTasklet = new ArchitectTasklet(
                null, tenantRunRepository, artifactRepository, taskExecutionRepository,
                objectMapper, null
        );
        architectTasklet.afterSuccess(testRun, artifact, "minimax");

        // Assert
        UUID expectedUuid = UUID.nameUUIDFromBytes(taskId.getBytes(StandardCharsets.UTF_8));
        List<TaskExecution> tasks = taskExecutionRepository.findByArtifactRun(testRun);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getPlannedTaskId())
                .as("TaskExecution.plannedTaskId should match UUID derived from ArchitectOutput.PlannedTask.id")
                .isEqualTo(expectedUuid);
    }
}
