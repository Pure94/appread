package pureapps.appread.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pureapps.appread.service.DocumentationGenerationService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentationController.class)
@Import(DocumentationControllerTest.TestConfig.class)
class DocumentationControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        DocumentationGenerationService documentationGenerationService() {
            return Mockito.mock(DocumentationGenerationService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentationGenerationService documentationGenerationService;

    @Test
    void generateDocumentation_ShouldReturnProjectId() throws Exception {
        // Arrange
        String repoUrl = "https://github.com/test/repo.git";
        String expectedProjectId = "repo-12345678";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("repoUrl", repoUrl);

        when(documentationGenerationService.generateDocumentation(
                eq(repoUrl), any(Optional.class), any(Optional.class)))
                .thenReturn(expectedProjectId);

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(expectedProjectId))
                .andExpect(jsonPath("$.message").value("Documentation generated successfully"));
    }

    @Test
    void generateDocumentation_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        // Missing required repoUrl

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateDocumentation_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String repoUrl = "https://github.com/test/repo.git";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("repoUrl", repoUrl);

        when(documentationGenerationService.generateDocumentation(
                eq(repoUrl), any(Optional.class), any(Optional.class)))
                .thenThrow(new RuntimeException("Failed to generate documentation"));

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Failed to generate documentation: Failed to generate documentation"));
    }

    @Test
    void generateLocalDocumentation_ShouldReturnDocumentationPath() throws Exception {
        // Arrange
        String localRepoPath = "/path/to/local/repo";
        Path expectedDocumentationPath = Path.of(localRepoPath, "documentation");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("localRepoPath", localRepoPath);

        when(documentationGenerationService.generateDocumentationForLocalRepo(any(Path.class)))
                .thenReturn(expectedDocumentationPath);

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate-local")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentationPath").value(expectedDocumentationPath.toString()))
                .andExpect(jsonPath("$.message").value("Documentation generated successfully"));
    }

    @Test
    void generateLocalDocumentation_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        // Missing required localRepoPath

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate-local")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateLocalDocumentation_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String localRepoPath = "/path/to/local/repo";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("localRepoPath", localRepoPath);

        when(documentationGenerationService.generateDocumentationForLocalRepo(any(Path.class)))
                .thenThrow(new RuntimeException("Failed to generate documentation"));

        // Act & Assert
        mockMvc.perform(post("/api/documentation/generate-local")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Failed to generate documentation for local repository: Failed to generate documentation"));
    }
}
