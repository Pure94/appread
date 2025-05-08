package pureapps.appread.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pureapps.appread.service.DocumentationGenerationService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for generating and retrieving documentation for repositories.
 */
@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationController {

    private final DocumentationGenerationService documentationGenerationService;

    /**
     * Generates documentation for a GitHub repository.
     *
     * @param request the request containing the repository URL and optional parameters
     * @return information about the generated documentation
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateDocumentation(@RequestBody GenerateDocumentationRequest request) {
        try {
            String projectId = documentationGenerationService.generateDocumentation(
                    request.getRepoUrl(),
                    Optional.ofNullable(request.getBranch()),
                    Optional.ofNullable(request.getToken())
            );

            Map<String, String> response = new HashMap<>();
            response.put("projectId", projectId);
            response.put("message", "Documentation generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate documentation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to generate documentation: " + e.getMessage());
        }
    }

    /**
     * Generates documentation for a local repository path.
     *
     * @param request the request containing the local repository path
     * @return information about the generated documentation
     */
    @PostMapping("/generate-local")
    public ResponseEntity<?> generateLocalDocumentation(@RequestBody GenerateLocalDocumentationRequest request) {
        try {
            Path documentationPath = documentationGenerationService.generateDocumentationForLocalRepo(
                    Path.of(request.getLocalRepoPath())
            );

            Map<String, String> response = new HashMap<>();
            response.put("documentationPath", documentationPath.toString());
            response.put("message", "Documentation generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate documentation for local repository: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to generate documentation for local repository: " + e.getMessage());
        }
    }

    /**
     * Request object for generating documentation from a GitHub repository
     */
    private static class GenerateDocumentationRequest {
        private String repoUrl;
        private String branch;
        private String token;

        public String getRepoUrl() {
            return repoUrl;
        }

        public void setRepoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
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

    /**
     * Request object for generating documentation from a local repository path
     */
    private static class GenerateLocalDocumentationRequest {
        private String localRepoPath;

        public String getLocalRepoPath() {
            return localRepoPath;
        }

        public void setLocalRepoPath(String localRepoPath) {
            this.localRepoPath = localRepoPath;
        }
    }
}