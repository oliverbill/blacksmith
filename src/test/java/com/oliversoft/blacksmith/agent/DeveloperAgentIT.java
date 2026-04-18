package com.oliversoft.blacksmith.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.model.dto.input.DeveloperInput;
import com.oliversoft.blacksmith.model.dto.output.ArchitectOutput;
import com.oliversoft.blacksmith.model.dto.output.ConstitutionOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import com.oliversoft.blacksmith.router.LLMRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * End-to-end integration test that calls the real LLM providers with a DeveloperInput
 * built from the sample JSON fixtures in src/test/resources.
 *
 * Uses the real Spring context — nothing is mocked.
 *
 * The test exercises task-1 from architect-output-sample.json:
 * "Add spring-ai-starter-model-deepseek dependency to pom.xml"
 */
@Nested
@SpringBootTest
@ActiveProfiles("test")
class DeveloperAgentIT {

    private static final Logger log = LoggerFactory.getLogger(DeveloperAgentIT.class);

    /** Repo URL passed to the LLM via allowedRepositoryUrls — the output repoUrl must match one of these. */
    private static final String ALLOWED_REPO_URL = "https://github.com/oliversoft/blacksmith";

    @Autowired
    private BlacksmithAgent agent;

    @Autowired
    private LLMRouter router;

    @Autowired
    private TaskExecutionRepository repository;

    @Autowired
    private RunArtifactRepository runArtifactRepository;

    @Autowired
    private TenantRunRepository tenantRunRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ConstitutionOutput constitution;
    private ArchitectOutput architectOutput;
    private ArchitectOutput.PlannedTask ongoingTask;

    @BeforeEach
    void loadSamples() throws Exception {
        this.constitution = this.objectMapper.readValue(
                new ClassPathResource("constitution-output-sample.json").getInputStream(),
                ConstitutionOutput.class
        );
        this.architectOutput = this.objectMapper.readValue(
                new ClassPathResource("architect-output-sample.json").getInputStream(),
                ArchitectOutput.class
        );

        this.ongoingTask = this.architectOutput.plannedTasks().get(0);
    }

    @Test
    void developerAgent_shouldGenerateCode_forAddDeepseekDependencyTask() {

        // Task-1: "Add spring-ai-starter-model-deepseek dependency to pom.xml"
        ArchitectOutput.PlannedTask task = architectOutput.plannedTasks().get(0);

        log.info("=== Running Developer agent for task: [{}] {} → {} ===",
                task.id(), task.description(), task.filenamePath());

        var input = new DeveloperInput(
                task,
                architectOutput,
                constitution,
                List.of(ALLOWED_REPO_URL),
                null   // no refinement feedback
        );

        BlacksmithAgent.AgentResult<DeveloperOutput> result =
                agent.processInput(input, AgentName.DEVELOPER, DeveloperOutput.class);

        // ── structural assertions ──────────────────────────────────────────

        assertThat(result).isNotNull();
        assertThat(result.output()).isNotNull();
        assertThat(result.providerName()).isNotBlank();

        log.info("=== Provider used: {} ===", result.providerName());

        DeveloperOutput output = result.output();

        // The agent must produce at least one file (either changed or new)
        boolean hasChangedFiles = output.changedFiles() != null && !output.changedFiles().isEmpty();
        boolean hasNewFiles     = output.newFiles()     != null && !output.newFiles().isEmpty();

        assertThat(hasChangedFiles || hasNewFiles)
                .as("DeveloperOutput must contain at least one changedFile or newFile")
                .isTrue();

        // ── per-file assertions ───────────────────────────────────────────

        List<DeveloperOutput.GeneratedFile> allFiles = new java.util.ArrayList<>();
        if (output.changedFiles() != null) allFiles.addAll(output.changedFiles());
        if (output.newFiles()     != null) allFiles.addAll(output.newFiles());

        log.info("=== Generated {} file(s) ===", allFiles.size());

        allFiles.forEach(file -> {
            log.info("  filePath={} | repoUrl={} | contentLength={}",
                    file.filePath(), file.repoUrl(), file.content() == null ? 0 : file.content().length());

            assertThat(file.filePath())
                    .as("Every generated file must have a non-blank filePath")
                    .isNotBlank();

            assertThat(file.content())
                    .as("Every generated file must have non-blank content")
                    .isNotBlank();

            assertThat(file.repoUrl())
                    .as("Every generated file must declare a repoUrl")
                    .isNotBlank();

            assertThat(file.repoUrl())
                    .as("repoUrl '%s' must be one of the allowedRepositoryUrls", file.repoUrl())
                    .isEqualTo(ALLOWED_REPO_URL);
        });

        // ── task-specific assertions ──────────────────────────────────────

        // Task-1 targets pom.xml: the file for that task should contain a deepseek dependency block
        DeveloperOutput.GeneratedFile pomFile = allFiles.stream()
                .filter(f -> f.filePath().endsWith("pom.xml"))
                .findFirst()
                .orElse(null);

        assertThat(pomFile)
                .as("Expected the agent to generate/modify pom.xml for the deepseek dependency task")
                .isNotNull();

        log.info("=== pom.xml content snippet (first 500 chars) ===\n{}",
                pomFile.content().substring(0, Math.min(500, pomFile.content().length())));

        assertThat(pomFile.content().toLowerCase())
                .as("pom.xml should reference deepseek in the new dependency")
                .contains("deepseek");

        assertThat(pomFile.content())
                .as("pom.xml content should look like valid Maven XML")
                .contains("<dependency>");
    }

    @Test
    void developerAgent_shouldGenerateCode_forCreateConfigClassTask() {

        // Task-2: "Create DeepSeekProperties configuration class in config package"
        ArchitectOutput.PlannedTask task = architectOutput.plannedTasks().get(1);

        log.info("=== Running Developer agent for task: [{}] {} → {} ===",
                task.id(), task.description(), task.filenamePath());

        var input = new DeveloperInput(
                task,
                architectOutput,
                constitution,
                List.of(ALLOWED_REPO_URL),
                null
        );

        BlacksmithAgent.AgentResult<DeveloperOutput> result =
                agent.processInput(input, AgentName.DEVELOPER, DeveloperOutput.class);

        assertThat(result).isNotNull();
        assertThat(result.output()).isNotNull();
        assertThat(result.providerName()).isNotBlank();

        log.info("=== Provider used: {} ===", result.providerName());

        DeveloperOutput output = result.output();

        List<DeveloperOutput.GeneratedFile> allFiles = new java.util.ArrayList<>();
        if (output.changedFiles() != null) allFiles.addAll(output.changedFiles());
        if (output.newFiles()     != null) allFiles.addAll(output.newFiles());

        assertThat(allFiles)
                .as("Agent must generate at least one file for the config class task")
                .isNotEmpty();

        allFiles.forEach(file -> {
            log.info("  filePath={} | repoUrl={} | contentLength={}",
                    file.filePath(), file.repoUrl(), file.content() == null ? 0 : file.content().length());

            assertThat(file.filePath()).isNotBlank();
            assertThat(file.content()).isNotBlank();
            assertThat(file.repoUrl()).isEqualTo(ALLOWED_REPO_URL);
        });

        // Task-2 targets a .java file: it should be valid Java source
        DeveloperOutput.GeneratedFile javaFile = allFiles.stream()
                .filter(f -> f.filePath().endsWith(".java"))
                .findFirst()
                .orElse(null);

        assertThat(javaFile)
                .as("Expected a .java file to be generated for the config class task")
                .isNotNull();

        log.info("=== Java file content snippet (first 500 chars) ===\n{}",
                javaFile.content().substring(0, Math.min(500, javaFile.content().length())));

        // The generated Java class should be a Spring @Configuration or @ConfigurationProperties
        assertThat(javaFile.content())
                .as("Generated Java class should contain a class declaration")
                .contains("class");

        assertThat(javaFile.content().toLowerCase())
                .as("DeepSeek config class should reference deepseek in some form")
                .containsAnyOf("deepseek", "deep_seek", "deep-seek");
    }

}
