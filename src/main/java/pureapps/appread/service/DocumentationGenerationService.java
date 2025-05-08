package pureapps.appread.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.DocumentVectorStorage;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service that connects GitService and DocumentVectorStorage to generate
 * documentation for a given GitHub repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationGenerationService {

    private final GitService gitService;
    private final DocumentVectorStorage documentVectorStorage;

    /**
     * Generates documentation for a GitHub repository.
     *
     * @param repoUrl GitHub repository URL
     * @param branch Optional branch name, defaults to the default branch if not provided
     * @param token Optional GitHub access token for private repositories
     * @return Project ID that can be used to query the generated documentation
     */
    public String generateDocumentation(String repoUrl, Optional<String> branch, Optional<String> token) {
        log.info("Generating documentation for repository: {}", repoUrl);
        
        try {
            // Generate a unique project ID
            String projectId = generateProjectId(repoUrl);
            
            // Clone the repository
            Path repoPath = gitService.cloneRepository(repoUrl, branch, token);
            
            // Process the repository and generate embeddings
            documentVectorStorage.generateEmbeddingsAndPersist(repoPath, projectId);
            
            // Clean up the repository files after processing
            gitService.deleteRepository(repoPath);
            
            log.info("Documentation generated successfully for repository: {}, project ID: {}", repoUrl, projectId);
            
            return projectId;
        } catch (Exception e) {
            log.error("Failed to generate documentation for repository: {}", repoUrl, e);
            throw new RuntimeException("Failed to generate documentation for repository: " + repoUrl, e);
        }
    }
    
    /**
     * Queries the documentation for a specific project.
     *
     * @param projectId Project ID returned by generateDocumentation
     * @param query Query string to search for in the documentation
     * @param limit Maximum number of results to return
     * @return List of document chunks matching the query
     */
    public List<DocumentChunk> queryDocumentation(String projectId, String query, int limit) {
        log.info("Querying documentation for project: {}, query: {}", projectId, query);
        
        try {
            List<DocumentChunk> results = documentVectorStorage.getDocumentChunksFromProject(projectId, query, limit);
            log.info("Found {} results for query: {}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("Failed to query documentation for project: {}, query: {}", projectId, query, e);
            throw new RuntimeException("Failed to query documentation for project: " + projectId, e);
        }
    }
    
    /**
     * Generates a unique project ID based on the repository URL.
     *
     * @param repoUrl GitHub repository URL
     * @return Unique project ID
     */
    private String generateProjectId(String repoUrl) {
        // Extract repository name from URL
        String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1)
                .replace(".git", "");
        
        // Combine repository name with a UUID to ensure uniqueness
        return repoName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}