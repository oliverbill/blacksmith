package com.oliversoft.blacksmith.tool;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BashTools {

    private static final Logger log = LoggerFactory.getLogger(BashTools.class);

    private final Path repoBaseFolder;

    public BashTools(@Value("${blacksmith.tenant.repo.basefolder}") String repoBaseFolder) {
        this.repoBaseFolder = Path.of(repoBaseFolder).toAbsolutePath().normalize();
    }

    @Tool(description = "Lists all files recursively in a directory. The path MUST be an absolute path obtained from the localRepoPaths field in the input.")
    public String listFiles(String filePath) {
        var validation = validate(filePath);
        if (validation != null) return validation;

        var path = Path.of(filePath).toAbsolutePath().normalize();
        List<String> collected = new ArrayList<>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    collected.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.debug("Skipping inaccessible path: {}", file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    collected.add(dir.toString());
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return "ERROR listing files: " + e.getMessage();
        }

        return collected.isEmpty() ? "Directory is empty." : String.join("\n", collected);
    }

    @Tool(description = "Reads the content of a file. Only use for small config files under 5KB")
    public String readFile(String path) {
        var validation = validate(path);
        if (validation != null) return validation;

        try {
            var file = Path.of(path);
            if (Files.isDirectory(file)) {
                return "Error: '" + path + "' is a directory. Use listFiles to list its contents.";
            }
            return Files.readString(file);
        } catch (IOException e) {
            log.error("Failed to read file: {}", path, e);
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Search for this pattern in this file")
    public String grep(String filePath, String pattern) {
        var validation = validate(filePath);
        if (validation != null) return validation;

        try {
            var file = Path.of(filePath);
            if (Files.isDirectory(file)) {
                return "Error: '" + filePath + "' is a directory. Use listFiles to list its contents.";
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

    @Tool(description = "Reads a specific section of a file from startLine to endLine.")
    public String readFileSection(String path, int startLine, int endLine) {
        var validation = validate(path);
        if (validation != null) return validation;

        try (var lines = Files.lines(Path.of(path))) {
            return lines
                .skip(startLine - 1)
                .limit(endLine - startLine + 1)
                .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Returns metadata of a file: number of lines, size in bytes, last modified.")
    public String getFileInfo(String path) {
        var validation = validate(path);
        if (validation != null) return validation;

        try {
            var file = Path.of(path);
            if (Files.isDirectory(file)) return "Is a directory";
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
