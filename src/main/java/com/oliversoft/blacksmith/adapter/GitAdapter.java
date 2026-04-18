package com.oliversoft.blacksmith.adapter;

import com.oliversoft.blacksmith.exception.PipelineExecutionException;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class GitAdapter {

    private static final Logger log = LoggerFactory.getLogger(GitAdapter.class);

    private final Path cloneBaseFolder;

    public GitAdapter(@Value("${blacksmith.tenant.repo.basefolder}") String cloneBaseFolder) {
        this.cloneBaseFolder = Path.of(cloneBaseFolder);
    }

    public Path cloneOrPull(String repoUrl) {
        try {
            String repoFolderName = sanitizeRepoName(repoUrl);
            Path targetDir = cloneBaseFolder.resolve(repoFolderName);

            Files.createDirectories(cloneBaseFolder);

            Path gitHead = targetDir.resolve(".git").resolve("HEAD");

            if (Files.exists(gitHead)) {
                log.info("Repository already cloned at {}. Pulling latest changes...", targetDir);
                try (Git git = Git.open(targetDir.toFile())) {
                    git.pull().call();
                }
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
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(targetDir.toFile())
                    .call()
                    .close();
            }

            return targetDir;

        } catch (PipelineExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineExecutionException("Failed to clone/pull repository: " + repoUrl, e);
        }
    }

    public void commitAndPush(String repoUrl, String message) {
        Path targetDir = getRepoLocalPath(repoUrl);
        try (Git git = Git.open(targetDir.toFile())) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage(message).call();
            git.push().call();
            log.info("Committed and pushed to {}: {}", repoUrl, message);
        } catch (Exception e) {
            throw new PipelineExecutionException("Failed to commit and push: " + repoUrl, e);
        }
    }

    public void createBranch(String repoUrl, String branchName) {
        Path targetDir = getRepoLocalPath(repoUrl);
        try (Git git = Git.open(targetDir.toFile())) {
            git.branchCreate().setName(branchName).call();
            git.checkout().setName(branchName).call();
            log.info("Created and checked out branch {} in {}", branchName, repoUrl);
        } catch (Exception e) {
            throw new PipelineExecutionException("Failed to create branch: " + branchName, e);
        }
    }

    public Path getRepoLocalPath(String repoUrl) {
        String repoFolderName = sanitizeRepoName(repoUrl);
        return cloneBaseFolder.resolve(repoFolderName);
    }

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