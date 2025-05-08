package pureapps.appread.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import pureapps.appread.documentsvectorstorage.DocumentVectorStorage;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;
import pureapps.appread.dto.FileNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentationGenerationServiceTest {

    @Mock
    private GitService gitService;

    @Mock
    private DocumentVectorStorage documentVectorStorage;

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private DocumentationGenerationService documentationGenerationService;

    @TempDir
    Path tempDir;

    private final String testRepoUrl = "https://github.com/test/repo.git";
    private final Path mockRepoPath = Path.of("mock/repo/path");
    private final String expectedProjectIdPrefix = "repo-";

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        // Common setup for tests
        when(gitService.cloneRepository(eq(testRepoUrl), any(), any()))
                .thenReturn(mockRepoPath);

        when(documentVectorStorage.generateEmbeddingsAndPersist(eq(mockRepoPath), anyString()))
                .thenReturn(List.of(new DocumentChunkWithEmbedding()));

        when(gitService.deleteRepository(mockRepoPath))
                .thenReturn(true);
    }

    @Test
    void generateDocumentation_ShouldReturnProjectId() throws GitAPIException, IOException {
        // Act
        String projectId = documentationGenerationService.generateDocumentation(
                testRepoUrl, Optional.empty(), Optional.empty());

        // Assert
        assertNotNull(projectId);
        assertTrue(projectId.startsWith(expectedProjectIdPrefix));

        // Verify interactions
        verify(gitService).cloneRepository(eq(testRepoUrl), eq(Optional.empty()), eq(Optional.empty()));
        verify(documentVectorStorage).generateEmbeddingsAndPersist(eq(mockRepoPath), eq(projectId));
        verify(gitService).deleteRepository(eq(mockRepoPath));
    }

    @Test
    void generateDocumentation_WithBranchAndToken_ShouldReturnProjectId() throws GitAPIException, IOException {
        // Arrange
        Optional<String> branch = Optional.of("develop");
        Optional<String> token = Optional.of("github_token");

        // Act
        String projectId = documentationGenerationService.generateDocumentation(
                testRepoUrl, branch, token);

        // Assert
        assertNotNull(projectId);
        assertTrue(projectId.startsWith(expectedProjectIdPrefix));

        // Verify interactions
        verify(gitService).cloneRepository(eq(testRepoUrl), eq(branch), eq(token));
        verify(documentVectorStorage).generateEmbeddingsAndPersist(eq(mockRepoPath), eq(projectId));
        verify(gitService).deleteRepository(eq(mockRepoPath));
    }

    @Test
    void generateDocumentation_WhenGitServiceThrowsException_ShouldPropagateException() throws GitAPIException, IOException {
        // Arrange
        when(gitService.cloneRepository(eq(testRepoUrl), any(), any()))
                .thenThrow(new RuntimeException("Failed to clone repository"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> 
                documentationGenerationService.generateDocumentation(testRepoUrl, Optional.empty(), Optional.empty()));

        assertTrue(exception.getMessage().contains("Failed to generate documentation"));

        // Verify no further interactions
        verify(documentVectorStorage, never()).generateEmbeddingsAndPersist(any(), any());
        verify(gitService, never()).deleteRepository(any());
    }

    @Test
    void queryDocumentation_ShouldReturnDocumentChunks() {
        // Arrange
        String projectId = "test-project-id";
        String query = "test query";
        int limit = 10;
        List<DocumentChunk> expectedChunks = List.of(new DocumentChunk());

        when(documentVectorStorage.getDocumentChunksFromProject(projectId, query, limit))
                .thenReturn(expectedChunks);

        // Act
        List<DocumentChunk> result = documentationGenerationService.queryDocumentation(projectId, query, limit);

        // Assert
        assertNotNull(result);
        assertEquals(expectedChunks, result);

        // Verify interactions
        verify(documentVectorStorage).getDocumentChunksFromProject(projectId, query, limit);
    }

    @Test
    void queryDocumentation_WhenDocumentVectorStorageThrowsException_ShouldPropagateException() {
        // Arrange
        String projectId = "test-project-id";
        String query = "test query";
        int limit = 10;

        when(documentVectorStorage.getDocumentChunksFromProject(projectId, query, limit))
                .thenThrow(new RuntimeException("Failed to query documentation"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> 
                documentationGenerationService.queryDocumentation(projectId, query, limit));

        assertTrue(exception.getMessage().contains("Failed to query documentation"));
    }

    @Test
    void generateDocumentationForLocalRepo_ShouldCreateDocumentationDirectory() throws IOException {
        // Arrange
        Path localRepoPath = tempDir;
        Path documentationPath = localRepoPath.resolve("documentation");
        FileNode mockFileStructure = new FileNode("root", localRepoPath.toString(), true);

        // Mock ChatClient response
        when(chatClient.prompt(any(Prompt.class)).call().content())
                .thenReturn("# Project Documentation\n\nThis is a test documentation.");

        // Mock GitService
        when(gitService.getFileStructure(localRepoPath)).thenReturn(mockFileStructure);

        // Act
        Path result = documentationGenerationService.generateDocumentationForLocalRepo(localRepoPath);

        // Assert
        assertEquals(documentationPath, result);
        assertTrue(Files.exists(documentationPath));

        // Verify interactions
        verify(gitService).getFileStructure(localRepoPath);
        verify(chatClient, atLeastOnce()).prompt(any(Prompt.class));
    }

    @Test
    void generateDocumentationForLocalRepo_WithInvalidPath_ShouldThrowException() {
        // Arrange
        Path invalidPath = Path.of("non-existent-path");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> 
                documentationGenerationService.generateDocumentationForLocalRepo(invalidPath));

        assertTrue(exception.getMessage().contains("Failed to generate documentation"));

        // Verify no interactions
        verify(gitService, never()).getFileStructure(any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void generateDocumentationForLocalRepo_WhenGitServiceThrowsException_ShouldPropagateException() {
        // Arrange
        Path localRepoPath = tempDir;

        when(gitService.getFileStructure(localRepoPath))
                .thenThrow(new RuntimeException("Failed to get file structure"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> 
                documentationGenerationService.generateDocumentationForLocalRepo(localRepoPath));

        assertTrue(exception.getMessage().contains("Failed to generate documentation"));

        // Verify no further interactions
        verify(chatClient, never()).prompt(any(Prompt.class));
    }
}
