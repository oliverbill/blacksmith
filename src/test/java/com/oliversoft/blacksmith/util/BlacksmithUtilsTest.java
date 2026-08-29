package com.oliversoft.blacksmith.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
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

    // ── cleanJson: no silent truncation of large/complex content ───────────────

    @Test
    void cleanJson_withNull_returnsEmptyString() {
        assertThat(BlacksmithUtils.cleanJson(null)).isEmpty();
    }

    @Test
    void cleanJson_withLargePlainObject_returnsItByteForByteUnchanged() {
        // Simulates a large DeveloperOutput with many full file contents — the exact scenario
        // where a naive length-limiting fix (substring/truncate) would silently corrupt data.
        String largeFileBody = "public class Big {\n" + "    // filler line\n".repeat(20_000) + "}";
        String json = "{\"changedFiles\":[{\"filePath\":\"Big.java\",\"content\":\"" + escapeJson(largeFileBody) + "\"}]}";
        assertThat(json.length()).isGreaterThan(300_000); // sanity: this really is a "large" payload

        String cleaned = BlacksmithUtils.cleanJson(json);

        assertThat(cleaned).isEqualTo(json);
        assertThat(cleaned.length()).isEqualTo(json.length());
    }

    @Test
    void cleanJson_stripsMarkdownFences_withoutTouchingInnerContent() {
        String inner = "{\"a\":1,\"b\":\"has ``` inside a string too\"}";
        String fenced = "```json\n" + inner + "\n```";

        String cleaned = BlacksmithUtils.cleanJson(fenced);

        // Current implementation strips ALL ``` occurrences, including ones inside string values —
        // documenting this as a known limitation rather than assuming it's safe.
        assertThat(cleaned).doesNotContain("```json");
        assertThat(cleaned).contains("\"a\":1");
    }

    @Test
    void cleanJson_unwrapsSingleElementObjectArray() {
        String obj = "{\"changedFiles\":[],\"newFiles\":[]}";
        String wrapped = "[" + obj + "]";

        assertThat(BlacksmithUtils.cleanJson(wrapped)).isEqualTo(obj);
    }

    @Test
    void cleanJson_withMultiElementArray_throwsInsteadOfSilentlyDroppingData() {
        // Jackson's readValue/readTree do NOT fail on trailing tokens by default — they silently
        // parse only the first object and discard the rest. Naively unwrapping [{...},{...}] into
        // "{...},{...}" would make the second object (and any files in it) vanish with zero trace.
        // cleanJson must refuse to do that and fail loudly instead, feeding the existing
        // retry/next-provider fallback that already handles malformed JSON.
        String multiElement = "[{\"changedFiles\":[]},{\"newFiles\":[{\"filePath\":\"Lost.java\"}]}]";

        assertThatThrownBy(() -> BlacksmithUtils.cleanJson(multiElement))
            .isInstanceOf(PipelineExecutionException.class)
            .hasMessageContaining("more than one object");
    }

    @Test
    void cleanJson_withNestedObjectsInsideSingleElement_stillUnwrapsCorrectly() {
        // Regression guard for the brace-depth counter added alongside the multi-element check:
        // nested {...} inside the single array element must not be mistaken for a second element.
        String obj = "{\"outer\":{\"inner\":{\"deep\":1}},\"list\":[1,2,3]}";
        String wrapped = "[" + obj + "]";

        assertThat(BlacksmithUtils.cleanJson(wrapped)).isEqualTo(obj);
    }

    @Test
    void cleanJson_withStringValueContainingBracesAndCommas_doesNotFalselyDetectMultipleObjects() {
        // The brace-depth counter must ignore braces/commas that appear inside string literals,
        // including escaped quotes within them.
        String obj = "{\"summary\":\"looks like an object } , { but it's just a string\",\"escaped\":\"a \\\" quote\"}";
        String wrapped = "[" + obj + "]";

        assertThat(BlacksmithUtils.cleanJson(wrapped)).isEqualTo(obj);
    }

    @Test
    void cleanJson_withPlainObjectNotWrapped_isReturnedUnchangedEvenIfItContainsBracketLikeText() {
        String json = "{\"summary\":\"the list is [1,2,3] and stays that way\"}";

        assertThat(BlacksmithUtils.cleanJson(json)).isEqualTo(json);
    }

    private static String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // ── isOutputValid: precise truncation/emptiness semantics ───────────────────

    @Test
    void isOutputValid_forNonDeveloperOutput_isAlwaysTrue() {
        var constitution = new ConstitutionOutput(null, List.of(), List.of(), List.of(), List.of(), List.of(),
            null, null, null, null);

        assertThat(BlacksmithUtils.isOutputValid(constitution)).isTrue();
    }

    @Test
    void isOutputValid_forDeveloperOutput_withBothListsEmpty_isFalse() {
        var output = new DeveloperOutput(List.of(), List.of());

        assertThat(BlacksmithUtils.isOutputValid(output)).isFalse();
    }

    @Test
    void isOutputValid_forDeveloperOutput_withBothListsNull_isFalse() {
        var output = new DeveloperOutput(null, null);

        assertThat(BlacksmithUtils.isOutputValid(output)).isFalse();
    }

    @Test
    void isOutputValid_forDeveloperOutput_withOnlyChangedFilesPopulated_isTrue() {
        var file = new DeveloperOutput.GeneratedFile("A.java", "class A {}", "https://example.com/r.git");
        var output = new DeveloperOutput(List.of(file), List.of());

        assertThat(BlacksmithUtils.isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_forDeveloperOutput_withOnlyNewFilesPopulated_isTrue() {
        var file = new DeveloperOutput.GeneratedFile("B.java", "class B {}", "https://example.com/r.git");
        var output = new DeveloperOutput(List.of(), List.of(file));

        assertThat(BlacksmithUtils.isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_forDeveloperOutput_withFileContentTruncatedToEmptyString_stillReportsValid() {
        // KNOWN GAP, pinned deliberately: isOutputValid only checks list emptiness, never whether
        // an individual GeneratedFile.content was itself cut short (e.g. by a provider hitting its
        // max-output-token limit mid-file, still yielding syntactically valid JSON). A file entry
        // with empty/truncated content passes this check. If this test starts failing because the
        // gap was closed, that's a genuine improvement — update the assertion, don't "fix" it back.
        var truncatedFile = new DeveloperOutput.GeneratedFile("Truncated.java", "", "https://example.com/r.git");
        var output = new DeveloperOutput(List.of(truncatedFile), List.of());

        assertThat(BlacksmithUtils.isOutputValid(output)).isTrue();
    }
}
