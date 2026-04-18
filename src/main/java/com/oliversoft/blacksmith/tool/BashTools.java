package com.oliversoft.blacksmith.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BashTools {

    private static final Logger log = LoggerFactory.getLogger(BashTools.class);

    private final Path repoBaseFolder;

    public BashTools(@Value("${blacksmith.tenant.repo.basefolder}") String repoBaseFolder) {
        this.repoBaseFolder = Path.of(repoBaseFolder).toAbsolutePath().normalize();
    }

    private static final java.util.Set<String> IGNORED_DIRS = java.util.Set.of(
        ".git", "target", "build", "out", ".idea", ".vscode", "node_modules", "__pycache__", ".gradle"
    );
    private static final java.util.Set<String> IGNORED_EXTENSIONS = java.util.Set.of(
        ".class", ".jar", ".war", ".ear", ".zip", ".tar", ".gz", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg"
    );

    @Tool(description = "Lists source files recursively in a directory, excluding build artifacts and binary files. The path MUST be an absolute path obtained from the localRepoPaths field in the input.")
    public String listFiles(String filePath) {
        var validation = validate(filePath);
        if (validation != null) return validation;

        var path = Path.of(filePath).toAbsolutePath().normalize();
        List<String> collected = new ArrayList<>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (IGNORED_DIRS.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                    if (!IGNORED_EXTENSIONS.contains(ext)) {
                        collected.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.debug("Skipping inaccessible path: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return "ERROR listing files: " + e.getMessage();
        }

        return collected.isEmpty() ? "Directory is empty." : String.join("\n", collected);
    }

    @Tool(description = "Reads the content of a file. Only use for small config files under 5KB. IMPORTANT: Only attempt to read files that you have confirmed exist by using listFiles first.")
    public String readFile(String path) {
        var validation = validate(path);
        if (validation != null) return validation;

        try {
            var file = Path.of(path);
            if (Files.isDirectory(file)) {
                return "Error: '" + path + "' is a directory. Use listFiles to list its contents.";
            }
            if (!Files.exists(file)) {
                return "Error: File '" + path + "' does not exist. Use listFiles to see available files and directories.";
            }
            return Files.readString(file);
        } catch (IOException e) {
            log.error("Failed to read file: {}", path, e);
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Search for this pattern in this file. IMPORTANT: Only search in files that you have confirmed exist by using listFiles first.")
    public String grep(String filePath, String pattern) {
        var validation = validate(filePath);
        if (validation != null) return validation;

        try {
            var file = Path.of(filePath);
            if (Files.isDirectory(file)) {
                return "Error: '" + filePath + "' is a directory. Use listFiles to list its contents.";
            }
            if (!Files.exists(file)) {
                return "Error: File '" + filePath + "' does not exist. Use listFiles to see available files and directories.";
            }

            var lines = Files.readAllLines(file);
            var result = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(pattern)) {
                    result.append("L").append(i + 1).append(": ").append(lines.get(i)).append("\n");
                }
            }
            return result.isEmpty() ? "No matches found." : result.toString();

        } catch (IOException e) {
            log.error("Failed to grep file: {}", filePath, e);
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = "Reads a specific section of a file from startLine to endLine. IMPORTANT: Only read from files that you have confirmed exist by using listFiles first.")
    public String readFileSection(String path, int startLine, int endLine) {
        var validation = validate(path);
        if (validation != null) return validation;

        var file = Path.of(path);
        if (!Files.exists(file)) {
            return "Error: File '" + path + "' does not exist. Use listFiles to see available files and directories.";
        }

        try (var lines = Files.lines(file)) {
            return lines
                .skip(startLine - 1)
                .limit(endLine - startLine + 1)
                .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Returns metadata of a file: number of lines, size in bytes, last modified. IMPORTANT: Only check files that you have confirmed exist by using listFiles first.")
    public String getFileInfo(String path) {
        var validation = validate(path);
        if (validation != null) return validation;

        try {
            var file = Path.of(path);
            if (Files.isDirectory(file)) return "Is a directory";
            if (!Files.exists(file)) {
                return "Error: File '" + path + "' does not exist. Use listFiles to see available files and directories.";
            }
            long lines = Files.lines(file).count();
            long bytes = Files.size(file);
            return "lines: " + lines + ", bytes: " + bytes;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private String validate(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "ERROR: path is empty.";
        }
        var path = Path.of(rawPath).toAbsolutePath().normalize();
        if (!path.startsWith(repoBaseFolder)) {
            return "ERROR: Access denied to '" + rawPath + "'. Tools are restricted to paths under " + repoBaseFolder
                    + ". Use the absolute path from the localRepoPaths input field.";
        }
        return null;
    }
}
