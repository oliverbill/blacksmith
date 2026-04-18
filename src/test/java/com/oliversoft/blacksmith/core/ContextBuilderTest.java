package com.oliversoft.blacksmith.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.model.dto.input.ArchitectInput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBuilderTest {

    private ContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilder(new ObjectMapper());
    }

    // ── getSystemPrompt ───────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(AgentName.class)
    void getSystemPrompt_forEachAgent_returnsNonEmptyPrompt(AgentName agent) {
        Optional<String> prompt = contextBuilder.getSystemPrompt(agent);

        assertThat(prompt).isPresent();
        assertThat(prompt.get()).isNotBlank();
    }

    @Test
    void getSystemPrompt_forConstitution_returnsSubstantialContent() {
        Optional<String> prompt = contextBuilder.getSystemPrompt(AgentName.CONSTITUTION);

        assertThat(prompt).isPresent();
        assertThat(prompt.get().length())
            .as("Constitution prompt should be a meaningful prompt, not a stub")
            .isGreaterThan(100);
    }

    @Test
    void getSystemPrompt_forArchitect_returnsSubstantialContent() {
        Optional<String> prompt = contextBuilder.getSystemPrompt(AgentName.ARCHITECT);

        assertThat(prompt).isPresent();
        assertThat(prompt.get().length()).isGreaterThan(100);
    }

    @Test
    void getSystemPrompt_forDeveloper_returnsSubstantialContent() {
        Optional<String> prompt = contextBuilder.getSystemPrompt(AgentName.DEVELOPER);

        assertThat(prompt).isPresent();
        assertThat(prompt.get().length()).isGreaterThan(100);
    }

    @Test
    void getSystemPrompt_forEachAgent_returnsDistinctPrompts() {
        String constitution = contextBuilder.getSystemPrompt(AgentName.CONSTITUTION).orElseThrow();
        String architect    = contextBuilder.getSystemPrompt(AgentName.ARCHITECT).orElseThrow();
        String developer    = contextBuilder.getSystemPrompt(AgentName.DEVELOPER).orElseThrow();

        assertThat(constitution).isNotEqualTo(architect);
        assertThat(constitution).isNotEqualTo(developer);
        assertThat(architect).isNotEqualTo(developer);
    }

    // ── buildUserPrompt ───────────────────────────────────────────────────────

    @Test
    void buildUserPrompt_withValidInput_returnsJsonString() {
        var constitution = new ConstitutionOutput(
            null, null, null, null, null, null, null, null, null, "test summary"
        );
        var input = new ArchitectInput(constitution, "add a Deepseek provider");

        String prompt = contextBuilder.buildUserPrompt(input);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("add a Deepseek provider");
        assertThat(prompt).contains("test summary");
    }

    @Test
    void buildUserPrompt_withNullFields_returnsValidJson() {
        var input = new ArchitectInput(null, "spec only");

        String prompt = contextBuilder.buildUserPrompt(input);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("spec only");
    }

    @Test
    void buildUserPrompt_producesValidJsonString() {
        var input = new ArchitectInput(null, "test spec");

        String prompt = contextBuilder.buildUserPrompt(input);

        assertThat(prompt).startsWith("{");
        assertThat(prompt).endsWith("}");
    }

    @Test
    void buildUserPrompt_withListFields_serializesCorrectly() {
        var constitution = new ConstitutionOutput(
            new ConstitutionOutput.DetectedStack("Java", List.of("Spring Boot"), "JUnit 5", "Maven", List.of("Lombok"), "PostgreSQL"),
            List.of("MVC", "Repository"),
            List.of("Tenant", "Run"),
            List.of("/api/runs", "/api/tenants"),
            List.of(),
            List.of(),
            "80%",
            new ConstitutionOutput.CodeConventions("SLF4J", "camelCase", "PipelineExecutionException"),
            new ConstitutionOutput.DependencyAudit(List.of(), List.of()),
            "Spring Boot AI pipeline"
        );
        var input = new ArchitectInput(constitution, "add feature");

        String prompt = contextBuilder.buildUserPrompt(input);

        assertThat(prompt).contains("Spring Boot");
        assertThat(prompt).contains("PostgreSQL");
        assertThat(prompt).contains("/api/runs");
    }
}
