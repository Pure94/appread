package pureapps.appread.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pureapps.appread.dto.FileNode;

@Service
@Slf4j
public class GitService {

    private final String tempRepoDir;
    private final Duration repositoryMaxAge;

    public GitService(
            @Value("${git.temp-repo-dir:temp-repos}") String tempRepoDir,
            @Value("${git.repo-max-age-hours:24}") int repoMaxAgeHours) {
        this.tempRepoDir = tempRepoDir;
        this.repositoryMaxAge = Duration.ofHours(repoMaxAgeHours);

        // Create the temp directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(tempRepoDir));
        } catch (IOException e) {
            log.error("Failed to create temporary repository directory: {}", e.getMessage(), e);
        }
    }

    /**
     * Clones a Git repository to a temporary directory
     *
     * @param repoUrl the URL of the repository to clone
     * @param branch optional branch to checkout
     * @param token optional authentication token for private repositories
     * @return the path to the cloned repository
     * @throws GitAPIException if there's an error with Git operations
     * @throws IOException if there's an I/O error
     */
    public Path cloneRepository(String repoUrl, Optional<String> branch, Optional<String> token)
            throws GitAPIException, IOException {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository URL cannot be null or empty");
        }

        // Extract repository name from URL
        String repoName;
        try {
            repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1)
                    .replace(".git", "");
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid repository URL format: " + repoUrl, e);
        }

        String uniqueDirName = repoName + "-" + System.currentTimeMillis();
        Path tempDir = Paths.get(tempRepoDir, uniqueDirName);

        try {
            Files.createDirectories(tempDir);

            log.info("Cloning repository {} to {}", repoUrl, tempDir);
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(repoUrl);
            cloneCommand.setDirectory(tempDir.toFile());

            token.ifPresent(s -> cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(s, "")));

            branch.ifPresent(cloneCommand::setBranch);

            try (Git git = cloneCommand.call()) {
                log.info("Repository cloned successfully");
                return tempDir;
            }
        } catch (Exception e) {
            // Clean up the temporary directory if cloning fails
            log.error("Failed to clone repository {}: {}", repoUrl, e.getMessage(), e);
            deleteRepository(tempDir);
            throw e;
        }
    }

    /**
     * Reads the content of a file from a cloned repository
     *
     * @param repoPath the path to the cloned repository
     * @param filePath the relative path of the file within the repository
     * @return the content of the file as a string
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the repository path or file path is invalid
     */
    public String readFile(Path repoPath, String filePath) throws IOException {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repository path cannot be null");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path fullPath = repoPath.resolve(filePath);

        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException("Repository does not exist: " + repoPath);
        }
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + fullPath);
        }
        if (!Files.isRegularFile(fullPath)) {
            throw new IOException("Path is not a regular file: " + fullPath);
        }

        try {
            return Files.readString(fullPath);
        } catch (IOException e) {
            log.error("Failed to read file {}: {}", fullPath, e.getMessage(), e);
            throw new IOException("Failed to read file: " + fullPath, e);
        }
    }

    /**
     * Lists all files and directories in the root of a cloned repository
     *
     * @param repoPath the path to the cloned repository
     * @return an array of files and directories in the repository root
     * @throws IllegalArgumentException if the repository path is invalid or does not exist
     */
    public File[] listFiles(Path repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repository path cannot be null");
        }

        File repoDir = repoPath.toFile();
        if (!repoDir.exists()) {
            throw new IllegalArgumentException("Repository does not exist: " + repoPath);
        }
        if (!repoDir.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + repoPath);
        }

        File[] files = repoDir.listFiles();
        if (files == null) {
            log.warn("Failed to list files in repository: {}", repoPath);
            return new File[0];
        }

        return files;
    }

    /**
     * Recursively lists all files in a repository
     *
     * @param repoPath the path to the cloned repository
     * @return a list of all files in the repository (excluding directories)
     * @throws IllegalArgumentException if the repository path is invalid or does not exist
     */
    public List<Path> listAllFiles(Path repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repository path cannot be null");
        }

        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException("Repository does not exist: " + repoPath);
        }
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + repoPath);
        }

        try (Stream<Path> pathStream = Files.walk(repoPath)) {
            return pathStream
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list all files in repository {}: {}", repoPath, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Recursively lists all files in a repository that match a specific file extension
     *
     * @param repoPath the path to the cloned repository
     * @param extension the file extension to filter by (e.g., ".java", ".txt")
     * @return a list of all files in the repository with the specified extension
     * @throws IllegalArgumentException if the repository path is invalid or does not exist
     */
    public List<Path> listFilesByExtension(Path repoPath, String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension cannot be null or empty");
        }

        String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;

        return listAllFiles(repoPath).stream()
            .filter(path -> path.toString().toLowerCase().endsWith(normalizedExtension.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Deletes a specific repository directory
     * 
     * @param repoPath the path to the repository to delete
     * @return true if the repository was successfully deleted, false otherwise
     */
    public boolean deleteRepository(Path repoPath) {
        if (repoPath == null || !Files.exists(repoPath)) {
            log.warn("Cannot delete repository: path is null or does not exist: {}", repoPath);
            return false;
        }

        log.info("Deleting repository at {}", repoPath);
        try {
            deleteDirectory(repoPath);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete repository at {}: {}", repoPath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cleans up repositories older than the configured maximum age
     * 
     * @return the number of repositories deleted
     */
    public int cleanupOldRepositories() {
        log.info("Cleaning up old repositories older than {} hours", repositoryMaxAge.toHours());
        Path repoDir = Paths.get(tempRepoDir);

        if (!Files.exists(repoDir)) {
            log.info("Repository directory does not exist: {}", repoDir);
            return 0;
        }

        Instant cutoffTime = Instant.now().minus(repositoryMaxAge);
        int deletedCount = 0;

        try (Stream<Path> paths = Files.list(repoDir)) {
            for (Path path : paths.toList()) {
                try {
                    if (!Files.isDirectory(path)) {
                        continue;
                    }

                    Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                    if (lastModified.isBefore(cutoffTime)) {
                        log.info("Deleting old repository: {} (last modified: {})", path, lastModified);
                        if (deleteRepository(path)) {
                            deletedCount++;
                        }
                    }
                } catch (IOException e) {
                    log.warn("Error processing repository path {}: {}", path, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to list repositories in {}: {}", repoDir, e.getMessage(), e);
        }

        log.info("Cleanup completed. Deleted {} old repositories", deletedCount);
        return deletedCount;
    }

    /**
     * Builds a hierarchical file/folder structure for a repository
     *
     * @param repoPath the path to the cloned repository
     * @return a FileNode representing the root of the repository with its file/folder structure
     * @throws IllegalArgumentException if the repository path is invalid or does not exist
     */
    public FileNode getFileStructure(Path repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repository path cannot be null");
        }

        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException("Repository does not exist: " + repoPath);
        }
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + repoPath);
        }

        // Create the root node
        File repoDir = repoPath.toFile();
        FileNode rootNode = new FileNode(repoDir.getName(), repoPath.toString(), true);

        try {
            // Build the file tree recursively
            buildFileTree(rootNode, repoPath);
            return rootNode;
        } catch (IOException e) {
            log.error("Failed to build file structure for repository {}: {}", repoPath, e.getMessage(), e);
            // Return the root node even if there was an error, it might be partially populated
            return rootNode;
        }
    }

    /**
     * Recursively builds the file tree starting from the given directory
     *
     * @param parentNode the parent node in the file tree
     * @param dirPath the path to the directory to process
     * @throws IOException if there's an I/O error
     */
    private void buildFileTree(FileNode parentNode, Path dirPath) throws IOException {
        try (Stream<Path> paths = Files.list(dirPath)) {
            List<Path> sortedPaths = paths
                .sorted((p1, p2) -> {
                    // Sort directories first, then files
                    boolean isDir1 = Files.isDirectory(p1);
                    boolean isDir2 = Files.isDirectory(p2);
                    if (isDir1 && !isDir2) {
                        return -1;
                    } else if (!isDir1 && isDir2) {
                        return 1;
                    } else {
                        // Both are directories or both are files, sort by name
                        return p1.getFileName().toString().compareTo(p2.getFileName().toString());
                    }
                })
                .collect(Collectors.toList());

            for (Path path : sortedPaths) {
                String name = path.getFileName().toString();

                // Skip .git directory and other hidden files/directories
                if (name.startsWith(".")) {
                    continue;
                }

                boolean isDirectory = Files.isDirectory(path);
                FileNode node = new FileNode(name, path.toString(), isDirectory);
                parentNode.addChild(node);

                // Recursively process subdirectories
                if (isDirectory) {
                    buildFileTree(node, path);
                }
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        // Use FileVisitor to handle file deletion with better error handling
        Files.walkFileTree(directory, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    log.warn("Failed to delete file {}: {}", file, e.getMessage());
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    log.warn("Failed to delete directory {}: {}", dir, e.getMessage());
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Failed to access file {}: {}", file, exc.getMessage());
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
}
