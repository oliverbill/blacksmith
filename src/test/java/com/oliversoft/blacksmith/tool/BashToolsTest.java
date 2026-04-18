package com.oliversoft.blacksmith.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BashToolsTest {

    @TempDir
    Path tempDir;

    private BashTools bashTools;

    @BeforeEach
    void setUp() {
        bashTools = new BashTools(tempDir.toString());
    }

    // ── listFiles ─────────────────────────────────────────────────────────────

    @Test
    void listFiles_withValidPath_returnsSourceFiles() throws IOException {
        Files.createFile(tempDir.resolve("App.java"));
        Files.createFile(tempDir.resolve("README.md"));

        String result = bashTools.listFiles(tempDir.toString());

        assertThat(result).contains("App.java");
        assertThat(result).contains("README.md");
    }

    @Test
    void listFiles_withPathOutsideSandbox_returnsAccessDeniedError() {
        String result = bashTools.listFiles("/etc/passwd");

        assertThat(result).startsWith("ERROR: Access denied");
        assertThat(result).contains("/etc/passwd");
    }

    @Test
    void listFiles_withNullPath_returnsEmptyPathError() {
        String result = bashTools.listFiles(null);

        assertThat(result).startsWith("ERROR: path is empty");
    }

    @Test
    void listFiles_withBlankPath_returnsEmptyPathError() {
        String result = bashTools.listFiles("   ");

        assertThat(result).startsWith("ERROR: path is empty");
    }

    @Test
    void listFiles_excludesIgnoredDirectories() throws IOException {
        Path targetDir = Files.createDirectory(tempDir.resolve("target"));
        Files.createFile(targetDir.resolve("App.class"));
        Files.createFile(tempDir.resolve("App.java"));

        String result = bashTools.listFiles(tempDir.toString());

        assertThat(result).contains("App.java");
        assertThat(result).doesNotContain("App.class");
    }

    @Test
    void listFiles_excludesIgnoredExtensions() throws IOException {
        Files.createFile(tempDir.resolve("App.class"));
        Files.createFile(tempDir.resolve("lib.jar"));
        Files.createFile(tempDir.resolve("photo.png"));
        Files.createFile(tempDir.resolve("App.java"));

        String result = bashTools.listFiles(tempDir.toString());

        assertThat(result).contains("App.java");
        assertThat(result).doesNotContain("App.class");
        assertThat(result).doesNotContain("lib.jar");
        assertThat(result).doesNotContain("photo.png");
    }

    @Test
    void listFiles_emptyDirectory_returnsEmptyMessage() throws IOException {
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty"));

        String result = bashTools.listFiles(emptyDir.toString());

        assertThat(result).isEqualTo("Directory is empty.");
    }

    @Test
    void listFiles_excludesNodeModules() throws IOException {
        Path nodeModules = Files.createDirectory(tempDir.resolve("node_modules"));
        Files.createFile(nodeModules.resolve("index.js"));
        Files.createFile(tempDir.resolve("app.js"));

        String result = bashTools.listFiles(tempDir.toString());

        assertThat(result).contains("app.js");
        assertThat(result).doesNotContain("index.js");
    }

    // ── readFile ──────────────────────────────────────────────────────────────

    @Test
    void readFile_withValidFile_returnsContent() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        String result = bashTools.readFile(file.toString());

        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void readFile_withPathOutsideSandbox_returnsAccessDeniedError() {
        String result = bashTools.readFile("/etc/hostname");

        assertThat(result).startsWith("ERROR: Access denied");
    }

    @Test
    void readFile_withNonExistentFile_returnsNotExistsError() {
        String result = bashTools.readFile(tempDir.resolve("nonexistent.txt").toString());

        assertThat(result).contains("does not exist");
    }

    @Test
    void readFile_withDirectory_returnsDirectoryError() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("subdir"));

        String result = bashTools.readFile(dir.toString());

        assertThat(result).contains("is a directory");
    }

    @Test
    void readFile_withNullPath_returnsEmptyPathError() {
        String result = bashTools.readFile(null);

        assertThat(result).startsWith("ERROR: path is empty");
    }

    // ── grep ──────────────────────────────────────────────────────────────────

    @Test
    void grep_withMatchingPattern_returnsMatchesWithLineNumbers() throws IOException {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "line one\nfoo bar\nline three\nfoo baz");

        String result = bashTools.grep(file.toString(), "foo");

        assertThat(result).contains("L2:");
        assertThat(result).contains("L4:");
        assertThat(result).contains("foo bar");
        assertThat(result).contains("foo baz");
    }

    @Test
    void grep_withNoMatches_returnsNoMatchesMessage() throws IOException {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "hello world");

        String result = bashTools.grep(file.toString(), "ZZZNOMATCH");

        assertThat(result).isEqualTo("No matches found.");
    }

    @Test
    void grep_withPathOutsideSandbox_returnsAccessDeniedError() {
        String result = bashTools.grep("/etc/passwd", "root");

        assertThat(result).startsWith("ERROR: Access denied");
    }

    @Test
    void grep_withNonExistentFile_returnsNotExistsError() {
        String result = bashTools.grep(tempDir.resolve("missing.txt").toString(), "pattern");

        assertThat(result).contains("does not exist");
    }

    @Test
    void grep_withDirectory_returnsDirectoryError() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("subdir"));

        String result = bashTools.grep(dir.toString(), "pattern");

        assertThat(result).contains("is a directory");
    }

    // ── readFileSection ───────────────────────────────────────────────────────

    @Test
    void readFileSection_withValidRange_returnsSpecifiedLines() throws IOException {
        Path file = tempDir.resolve("multiline.txt");
        Files.writeString(file, "line1\nline2\nline3\nline4\nline5");

        String result = bashTools.readFileSection(file.toString(), 2, 4);

        assertThat(result).isEqualTo("line2\nline3\nline4");
    }

    @Test
    void readFileSection_withPathOutsideSandbox_returnsAccessDeniedError() {
        String result = bashTools.readFileSection("/etc/passwd", 1, 2);

        assertThat(result).startsWith("ERROR: Access denied");
    }

    @Test
    void readFileSection_withNonExistentFile_returnsError() {
        String result = bashTools.readFileSection(tempDir.resolve("missing.txt").toString(), 1, 2);

        assertThat(result).contains("does not exist");
    }

    // ── getFileInfo ───────────────────────────────────────────────────────────

    @Test
    void getFileInfo_withValidFile_returnsMetadata() throws IOException {
        Path file = tempDir.resolve("info.txt");
        Files.writeString(file, "hello\nworld\n");

        String result = bashTools.getFileInfo(file.toString());

        assertThat(result).contains("lines:");
        assertThat(result).contains("bytes:");
    }

    @Test
    void getFileInfo_withPathOutsideSandbox_returnsAccessDeniedError() {
        String result = bashTools.getFileInfo("/etc/hostname");

        assertThat(result).startsWith("ERROR: Access denied");
    }

    @Test
    void getFileInfo_withDirectory_returnsDirectoryMessage() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("subdir"));

        String result = bashTools.getFileInfo(dir.toString());

        assertThat(result).isEqualTo("Is a directory");
    }

    @Test
    void getFileInfo_withNonExistentFile_returnsError() {
        String result = bashTools.getFileInfo(tempDir.resolve("missing.txt").toString());

        assertThat(result).contains("does not exist");
    }
}
