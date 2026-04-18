package com.oliversoft.blacksmith.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.entity.RunArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlacksmithUtilsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ── toJSON ────────────────────────────────────────────────────────────────

    @Test
    void toJSON_withValidOutput_returnsNonBlankJsonString() {
        var output = new ArchitectOutput(
            new ArchitectOutput.ChangeManagementPlan("Title", "Detail", List.of(), List.of(), List.of(), List.of()),
            List.of()
        );

        String json = BlacksmithUtils.toJSON(output);

        assertThat(json).isNotBlank();
        assertThat(json).contains("Title");
        assertThat(json).contains("Detail");
    }

    @Test
    void toJSON_withPlannedTasks_includesTasksInJson() {
        var task = new ArchitectOutput.PlannedTask("task-1", "Implement feature", "src/Main.java", List.of());
        var output = new ArchitectOutput(
            new ArchitectOutput.ChangeManagementPlan("T", "D", List.of(), List.of(), List.of(), List.of()),
            List.of(task)
        );

        String json = BlacksmithUtils.toJSON(output);

        assertThat(json).contains("task-1");
        assertThat(json).contains("Implement feature");
        assertThat(json).contains("src/Main.java");
    }

    // ── getJsonOutputByArtifact ───────────────────────────────────────────────

    @Test
    void getJsonOutputByArtifact_withValidJson_returnsDeserializedOutput() throws Exception {
        var output = new ArchitectOutput(
            new ArchitectOutput.ChangeManagementPlan("T", "D", List.of(), List.of(), List.of(), List.of()),
            List.of(new ArchitectOutput.PlannedTask("id1", "desc", "path/to/File.java", List.of()))
        );
        String json = objectMapper.writeValueAsString(output);

        RunArtifact artifact = mock(RunArtifact.class);
        when(artifact.getContent()).thenReturn(json);

        ArchitectOutput result = (ArchitectOutput) BlacksmithUtils.getJsonOutputByArtifact(artifact, ArchitectOutput.class);

        assertThat(result.plan().changeTitle()).isEqualTo("T");
        assertThat(result.plannedTasks()).hasSize(1);
        assertThat(result.plannedTasks().get(0).id()).isEqualTo("id1");
        assertThat(result.plannedTasks().get(0).filenamePath()).isEqualTo("path/to/File.java");
    }

    @Test
    void getJsonOutputByArtifact_withInvalidJson_throwsPipelineExecutionException() {
        RunArtifact artifact = mock(RunArtifact.class);
        when(artifact.getContent()).thenReturn("{not valid json!!!");

        assertThatThrownBy(() -> BlacksmithUtils.getJsonOutputByArtifact(artifact, ArchitectOutput.class))
            .isInstanceOf(PipelineExecutionException.class);
    }

    @Test
    void getJsonOutputByArtifact_withEmptyJson_throwsPipelineExecutionException() {
        RunArtifact artifact = mock(RunArtifact.class);
        when(artifact.getContent()).thenReturn("");

        assertThatThrownBy(() -> BlacksmithUtils.getJsonOutputByArtifact(artifact, ArchitectOutput.class))
            .isInstanceOf(PipelineExecutionException.class);
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    void toJSON_thenGetJsonOutputByArtifact_roundTripsCorrectly() {
        var original = new ArchitectOutput(
            new ArchitectOutput.ChangeManagementPlan("Round Trip", "Details", List.of("file.java"), List.of("new.java"), List.of(), List.of("risk1")),
            List.of(
                new ArchitectOutput.PlannedTask("t1", "Task one", "src/One.java", List.of()),
                new ArchitectOutput.PlannedTask("t2", "Task two", "src/Two.java", List.of("t1"))
            )
        );

        String json = BlacksmithUtils.toJSON(original);
        RunArtifact artifact = mock(RunArtifact.class);
        when(artifact.getContent()).thenReturn(json);

        ArchitectOutput restored = (ArchitectOutput) BlacksmithUtils.getJsonOutputByArtifact(artifact, ArchitectOutput.class);

        assertThat(restored.plan().changeTitle()).isEqualTo("Round Trip");
        assertThat(restored.plan().risks()).containsExactly("risk1");
        assertThat(restored.plannedTasks()).hasSize(2);
        assertThat(restored.plannedTasks().get(1).dependentTasks()).containsExactly("t1");
    }
}
