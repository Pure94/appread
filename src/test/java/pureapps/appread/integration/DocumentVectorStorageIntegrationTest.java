package pureapps.appread.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pureapps.appread.documentsvectorstorage.DocumentVectorStorage;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class DocumentVectorStorageIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                    .withDatabaseName("testdb")
                    .withUsername("postgres")
                    .withPassword("test")
                    .withInitScript("init-scripts/01-init-pgvector.sql");


    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
    }

    @Autowired
    private DocumentVectorStorage documentVectorStorage;

    @TempDir
    Path tempProjectDir;

    @BeforeEach
    void setUp() throws IOException {
        createTestProjectStructure();
    }

    private void createTestProjectStructure() throws IOException {
        // Create a simple Java file
        Path javaFilePath = tempProjectDir.resolve("TestFile.java");
        String javaContent = """
                public class TestFile {
                    public static void main(String[] args) {
                        System.out.println("Hello, world!");
                    }
                }""";
        Files.writeString(javaFilePath, javaContent);

        // Create a simple text file
        Path textFilePath = tempProjectDir.resolve("README.md");
        String textContent = """
                # Test Project
                
                This is a test project for integration testing.
                
                ## Features
                
                - Feature 1
                - Feature 2
                """;
        Files.writeString(textFilePath, textContent);
    }

    @Test
    void testGenerateEmbeddingsAndPersist() {
        // Generate embeddings and persist
        List<DocumentChunkWithEmbedding> result = documentVectorStorage.generateEmbeddingsAndPersist(tempProjectDir);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verify that the chunks contain the expected content
        boolean foundJavaFile = false;
        boolean foundReadmeFile = false;

        for (DocumentChunkWithEmbedding chunk : result) {
            if (chunk.getFilePath().endsWith("TestFile.java")) {
                foundJavaFile = true;
                assertTrue(chunk.getContent().contains("public class TestFile"));
            } else if (chunk.getFilePath().endsWith("README.md")) {
                foundReadmeFile = true;
                assertTrue(chunk.getContent().contains("# Test Project"));
            }

            assertNotNull(chunk.getEmbedding());
            assertEquals(1536, chunk.getEmbedding().length);
        }

        assertTrue(foundJavaFile, "Should have found the Java file");
        assertTrue(foundReadmeFile, "Should have found the README file");
    }

    @Test
    void testGetDocumentChunksFromProject() {
        // First, generate embeddings and persist
        documentVectorStorage.generateEmbeddingsAndPersist(tempProjectDir);

        // Now query for chunks
        String projectId = "your-project-id";
        String query = "Hello, world";
        int limit = 5;

        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(projectId, query, limit);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verify that the chunks are from the expected project
        for (DocumentChunk chunk : result) {
            assertTrue(chunk.getFilePath().contains("TestFile.java") ||
                            chunk.getFilePath().contains("README.md"),
                    "Chunk should be from one of the test files: " + chunk.getFilePath());
        }
    }

    @Test
    void testNonExistentProject() {
        // First, generate embeddings and persist
        documentVectorStorage.generateEmbeddingsAndPersist(tempProjectDir);

        // Now query for chunks from a non-existent project
        String nonExistentProjectId = "non-existent-project";
        String query = "Hello, world";
        int limit = 5;

        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(nonExistentProjectId, query, limit);

        // Verify the result is empty
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should not find any chunks for a non-existent project");
    }
}
