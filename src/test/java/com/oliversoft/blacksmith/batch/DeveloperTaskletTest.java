package com.oliversoft.blacksmith.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliversoft.blacksmith.adapter.GitAdapter;
import com.oliversoft.blacksmith.agent.BlacksmithAgent;
import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import com.oliversoft.blacksmith.inputbuilder.InputBuilderRegistry;
import com.oliversoft.blacksmith.model.dto.output.AgentOutput;
import com.oliversoft.blacksmith.model.dto.output.DeveloperOutput;
import com.oliversoft.blacksmith.model.enumeration.AgentName;
import com.oliversoft.blacksmith.model.enumeration.ArtifactType;
import com.oliversoft.blacksmith.persistence.RunArtifactRepository;
import com.oliversoft.blacksmith.persistence.TaskExecutionRepository;
import com.oliversoft.blacksmith.persistence.TenantRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.oliversoft.blacksmith.util.BlacksmithUtils.isOutputValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DeveloperTasklet.
 *
 * Tests are in the same package to access the protected isOutputValid method.
 * writeFilesToLocalRepo is tested indirectly via the file-writing behaviour exercised
 * in the package-visible helper.
 */
class DeveloperTaskletTest {

    private DeveloperTasklet tasklet;
    private GitAdapter gitAdapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitAdapter = mock(GitAdapter.class);

        tasklet = new DeveloperTasklet(
            mock(BlacksmithAgent.class),
            mock(TenantRunRepository.class),
            mock(RunArtifactRepository.class),
            mock(TaskExecutionRepository.class),
            new ObjectMapper(),
            mock(InputBuilderRegistry.class),
            gitAdapter
        );
    }

    // ── metadata ──────────────────────────────────────────────────────────────

    @Test
    void getAgentName_returnsDeveloper() {
        assertThat(tasklet.getAgentName()).isEqualTo(AgentName.DEVELOPER);
    }

    @Test
    void getArtifactType_returnsCode() {
        assertThat(tasklet.getArtifactType()).isEqualTo(ArtifactType.CODE);
    }

    @Test
    void allowsDuplicateArtifacts_returnsTrue() {
        assertThat(tasklet.allowsDuplicateArtifacts()).isTrue();
    }

    @Test
    void getRepeatStatus_returnsContinuable() {
        assertThat(tasklet.getRepeatStatus()).isEqualTo(org.springframework.batch.repeat.RepeatStatus.CONTINUABLE);
    }

    // ── isOutputValid ─────────────────────────────────────────────────────────

    @Test
    void isOutputValid_withChangedFiles_returnsTrue() {
        var output = new DeveloperOutput(
            List.of(new DeveloperOutput.GeneratedFile("src/Main.java", "class Main {}", "https://github.com/repo")),
            List.of()
        );

        assertThat(isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_withNewFiles_returnsTrue() {
        var output = new DeveloperOutput(
            List.of(),
            List.of(new DeveloperOutput.GeneratedFile("src/NewFeature.java", "public class NewFeature {}", "https://github.com/repo"))
        );

        assertThat(isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_withBothChangedAndNewFiles_returnsTrue() {
        var output = new DeveloperOutput(
            List.of(new DeveloperOutput.GeneratedFile("src/A.java", "class A {}", "url")),
            List.of(new DeveloperOutput.GeneratedFile("src/B.java", "class B {}", "url"))
        );

        assertThat(isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_withEmptyLists_returnsFalse() {
        var output = new DeveloperOutput(List.of(), List.of());

        assertThat(isOutputValid(output)).isFalse();
    }

    @Test
    void isOutputValid_withNullChangedAndNullNew_returnsFalse() {
        var output = new DeveloperOutput(null, null);

        assertThat(isOutputValid(output)).isFalse();
    }

    @Test
    void isOutputValid_withNullChangedButHasNew_returnsTrue() {
        var output = new DeveloperOutput(
            null,
            List.of(new DeveloperOutput.GeneratedFile("src/New.java", "class New {}", "url"))
        );

        assertThat(isOutputValid(output)).isTrue();
    }

    @Test
    void isOutputValid_withHasChangedButNullNew_returnsTrue() {
        var output = new DeveloperOutput(
            List.of(new DeveloperOutput.GeneratedFile("src/Existing.java", "class Existing {}", "url")),
            null
        );

        assertThat(isOutputValid(output)).isTrue();
    }

    // ── writeFilesToLocalRepo (via package-level access) ──────────────────────

    @Test
    void writeFiles_withAllowedUrl_writesFileToRepoPath() throws Exception {
        String repoUrl = "https://github.com/myorg/myrepo";
        when(gitAdapter.getRepoLocalPath(repoUrl)).thenReturn(tempDir);

        var file = new DeveloperOutput.GeneratedFile("src/Hello.java", "public class Hello {}", repoUrl);
        List<Path> written = new ArrayList<>();

        // Access the private method via the package-level test in same package.
        // We call writeFilesToLocalRepo via reflection to test file writing.
        var method = DeveloperTasklet.class.getDeclaredMethod(
            "writeFilesToLocalRepo", List.class, List.class, List.class);
        method.setAccessible(true);
        method.invoke(tasklet, List.of(file), List.of(repoUrl), written);

        assertThat(written).hasSize(1);
        Path expectedFile = tempDir.resolve("src/Hello.java");
        assertThat(expectedFile).exists();
        assertThat(Files.readString(expectedFile)).isEqualTo("public class Hello {}");
    }

    @Test
    void writeFiles_withDisallowedUrl_throwsPipelineExecutionException() throws Exception {
        var method = DeveloperTasklet.class.getDeclaredMethod(
            "writeFilesToLocalRepo", List.class, List.class, List.class);
        method.setAccessible(true);

        var file = new DeveloperOutput.GeneratedFile("src/Evil.java", "class Evil {}", "https://evil.com/repo");
        List<Path> written = new ArrayList<>();

        try {
            method.invoke(tasklet, List.of(file), List.of("https://github.com/allowed/repo"), written);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getCause()).isInstanceOf(PipelineExecutionException.class);
            assertThat(e.getCause().getMessage()).contains("not in the tenant's allowed repositories");
        }
    }

    @Test
    void writeFiles_withPathTraversal_throwsPipelineExecutionException() throws Exception {
        String repoUrl = "https://github.com/myorg/myrepo";
        when(gitAdapter.getRepoLocalPath(repoUrl)).thenReturn(tempDir);

        var method = DeveloperTasklet.class.getDeclaredMethod(
            "writeFilesToLocalRepo", List.class, List.class, List.class);
        method.setAccessible(true);

        // Attempt directory traversal
        var file = new DeveloperOutput.GeneratedFile("../../etc/passwd", "malicious content", repoUrl);
        List<Path> written = new ArrayList<>();

        try {
            method.invoke(tasklet, List.of(file), List.of(repoUrl), written);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getCause()).isInstanceOf(PipelineExecutionException.class);
            assertThat(e.getCause().getMessage()).contains("outside repository");
        }
    }

    @Test
    void writeFiles_withEmptyList_writesNothing() throws Exception {
        var method = DeveloperTasklet.class.getDeclaredMethod(
            "writeFilesToLocalRepo", List.class, List.class, List.class);
        method.setAccessible(true);

        List<Path> written = new ArrayList<>();
        method.invoke(tasklet, List.of(), List.of("https://github.com/repo"), written);

        assertThat(written).isEmpty();
    }

    @Test
    void writeFiles_createsIntermediateDirectories() throws Exception {
        String repoUrl = "https://github.com/myorg/myrepo";
        when(gitAdapter.getRepoLocalPath(repoUrl)).thenReturn(tempDir);

        var method = DeveloperTasklet.class.getDeclaredMethod(
            "writeFilesToLocalRepo", List.class, List.class, List.class);
        method.setAccessible(true);

        var file = new DeveloperOutput.GeneratedFile(
            "src/main/java/com/example/Service.java", "public class Service {}", repoUrl);
        List<Path> written = new ArrayList<>();
        method.invoke(tasklet, List.of(file), List.of(repoUrl), written);

        Path expectedFile = tempDir.resolve("src/main/java/com/example/Service.java");
        assertThat(expectedFile).exists();
    }
}
