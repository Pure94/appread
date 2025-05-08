package pureapps.appread.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pureapps.appread.dto.FileNode;
import pureapps.appread.service.GitService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/repository")
@RequiredArgsConstructor
@Slf4j
public class RepositoryController {

    private final GitService gitService;

    /**
     * Clones a Git repository
     *
     * @param request the request containing the repository URL and optional token
     * @return information about the cloned repository
     */
    @PostMapping("/clone")
    public ResponseEntity<?> cloneRepository(@RequestBody CloneRepositoryRequest request) {
        try {
            Path repoPath = gitService.cloneRepository(
                    request.getUrl(),
                    Optional.ofNullable(request.getBranch()),
                    Optional.ofNullable(request.getToken())
            );

            Map<String, String> response = new HashMap<>();
            response.put("name", repoPath.getFileName().toString());
            response.put("path", repoPath.toString());

            return ResponseEntity.ok(response);
        } catch (GitAPIException | IOException e) {
            log.error("Failed to clone repository: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to clone repository: " + e.getMessage());
        }
    }

    /**
     * Gets the file structure of a repository
     *
     * @param path the path to the repository
     * @return the file structure as a hierarchical tree
     */
    @GetMapping("/files")
    public ResponseEntity<?> getFileStructure(@RequestParam String path) {
        try {
            FileNode fileStructure = gitService.getFileStructure(Path.of(path));
            return ResponseEntity.ok(fileStructure);
        } catch (IllegalArgumentException e) {
            log.error("Failed to get file structure: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to get file structure: " + e.getMessage());
        }
    }

    /**
     * Request object for cloning a repository
     */
    private static class CloneRepositoryRequest {
        private String url;
        private String branch;
        private String token;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}