package com.oliversoft.blacksmith.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oliversoft.blacksmith.exception.PipelineExecutionException;

@Component
public class GitCloner {

    private static final Logger log = LoggerFactory.getLogger(GitCloner.class);

    private final Path cloneBaseFolder;

    public GitCloner(@Value("${blacksmith.tenant.repo.basefolder}") String cloneBaseFolder) {
        this.cloneBaseFolder = Path.of(cloneBaseFolder);
    }

    /**
     * Clones a git repository and returns the local path.
     * If the repository already exists, it will be updated (git pull).
     *
     * @param repoUrl The URL of the git repository (https://github.com/...)
     * @return The local path to the cloned repository
     */
    public Path cloneOrPull(String repoUrl) {
        try {
            String repoFolderName = sanitizeRepoName(repoUrl);
            Path targetDir = cloneBaseFolder.resolve(repoFolderName);

            Files.createDirectories(cloneBaseFolder);

            Path gitHead = targetDir.resolve(".git").resolve("HEAD");

            if (Files.exists(gitHead)) {
                log.info("Repository already cloned at {}. Pulling latest changes...", targetDir);
                runGit(targetDir, "git", "pull");
            } else {
                if (Files.exists(targetDir)) {
                    log.warn("Directory {} exists but is not a valid git repo. Deleting for fresh clone.", targetDir);
                    try (var walk = Files.walk(targetDir)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try { Files.delete(p); } catch (Exception ignored) {}
                        });
                    }
                }
                log.info("Cloning {} to {}", repoUrl, targetDir);
                runGit(cloneBaseFolder, "git", "clone", repoUrl, targetDir.toString());
            }

            return targetDir;

        } catch (PipelineExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineExecutionException("Failed to clone repository: " + repoUrl, e);
        }
    }

    private void runGit(Path workingDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PipelineExecutionException(
                    "git command failed (exit " + exitCode + "): " + String.join(" ", command) + "\n" + output);
            }

            log.debug("git output: {}", output);

        } catch (PipelineExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineExecutionException("Failed to run git command: " + String.join(" ", command), e);
        }
    }

    public Path getRepoLocalPath(String repoUrl) {
        String repoFolderName = sanitizeRepoName(repoUrl);
        return cloneBaseFolder.resolve(repoFolderName);
    }

    /**
     * Sanitizes a repo URL into a safe folder name.
     * e.g., https://github.com/oliverbill/blacksmith -> github-com-oliverbill-blacksmith
     */
    private String sanitizeRepoName(String repoUrl) {
        try {
            URI uri = new URI(repoUrl);
            String host = uri.getHost() != null ? uri.getHost().replace(".", "-") : "unknown";
            String path = uri.getPath() != null ? uri.getPath().replace("/", "-").replace(".git", "") : "repo";
            String name = (host + path).replaceAll("^-+|-+$", "").replaceAll("-+", "-");
            return name;
        } catch (Exception e) {
            log.warn("Failed to parse URL {}: {}", repoUrl, e.getMessage());
            return "repo-" + Integer.toHexString(repoUrl.hashCode());
        }
    }
}
