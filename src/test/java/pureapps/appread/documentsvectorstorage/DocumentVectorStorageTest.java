package pureapps.appread.documentsvectorstorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentVectorStorageTest {

    private DocumentProcessingService documentProcessingService;
    private EmbeddingService embeddingService;
    private PersistenceService persistenceService;
    private DocumentVectorStorage documentVectorStorage;
    private float similarityThreshold = 0.7f;

    @BeforeEach
    void setUp() throws Exception {
        // Create mocks for dependencies
        documentProcessingService = Mockito.mock(DocumentProcessingService.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        persistenceService = Mockito.mock(PersistenceService.class);

        // Create instance of DocumentVectorStorage with mocked dependencies
        documentVectorStorage = new DocumentVectorStorage(persistenceService, documentProcessingService, embeddingService);

        // Set the similarityThreshold field using reflection
        Field thresholdField = DocumentVectorStorage.class.getDeclaredField("similarityThreshold");
        thresholdField.setAccessible(true);
        thresholdField.set(documentVectorStorage, similarityThreshold);
    }

    @Test
    void testGenerateEmbeddingsAndPersist() throws Exception {
        // Create test data
        Path projectPath = Path.of("test-project");

        // Create test document chunks
        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setContent("Test content 1");
        chunk1.setFilePath("test/path1.java");
        chunk1.setStartLine(1);
        chunk1.setEndLine(10);

        DocumentChunk chunk2 = new DocumentChunk();
        chunk2.setContent("Test content 2");
        chunk2.setFilePath("test/path2.java");
        chunk2.setStartLine(11);
        chunk2.setEndLine(20);

        List<DocumentChunk> chunks = Arrays.asList(chunk1, chunk2);

        // Create test document chunks with embeddings
        DocumentChunkWithEmbedding chunkWithEmbedding1 = new DocumentChunkWithEmbedding();
        chunkWithEmbedding1.setContent("Test content 1");
        chunkWithEmbedding1.setFilePath("test/path1.java");
        chunkWithEmbedding1.setStartLine(1);
        chunkWithEmbedding1.setEndLine(10);
        chunkWithEmbedding1.setEmbedding(new float[1536]);

        DocumentChunkWithEmbedding chunkWithEmbedding2 = new DocumentChunkWithEmbedding();
        chunkWithEmbedding2.setContent("Test content 2");
        chunkWithEmbedding2.setFilePath("test/path2.java");
        chunkWithEmbedding2.setStartLine(11);
        chunkWithEmbedding2.setEndLine(20);
        chunkWithEmbedding2.setEmbedding(new float[1536]);

        List<DocumentChunkWithEmbedding> chunksWithEmbeddings = Arrays.asList(chunkWithEmbedding1, chunkWithEmbedding2);

        // Mock the behavior of dependencies
        when(documentProcessingService.processProjectToChunks(projectPath)).thenReturn(chunks);
        when(embeddingService.generateEmbeddings(chunks)).thenReturn(chunksWithEmbeddings);
        doNothing().when(persistenceService).saveChunks(anyString(), eq(chunksWithEmbeddings));

        // Call the method under test
        String projectId = "test-project-id";
        List<DocumentChunkWithEmbedding> result = documentVectorStorage.generateEmbeddingsAndPersist(projectPath, projectId);

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test content 1", result.get(0).getContent());
        assertEquals("test/path1.java", result.get(0).getFilePath());
        assertEquals(1, result.get(0).getStartLine());
        assertEquals(10, result.get(0).getEndLine());
        assertNotNull(result.get(0).getEmbedding());

        // Verify that the dependencies were called with the expected arguments
        verify(documentProcessingService).processProjectToChunks(projectPath);
        verify(embeddingService).generateEmbeddings(chunks);
        verify(persistenceService).saveChunks(eq(projectId), eq(chunksWithEmbeddings));
    }

    @Test
    void testGenerateEmbeddingsAndPersistWithEmptyChunks() throws Exception {
        // Create test data
        Path projectPath = Path.of("empty-project");
        String projectId = "empty-project-id";

        // Mock the behavior of dependencies
        when(documentProcessingService.processProjectToChunks(projectPath)).thenReturn(List.of());

        // Call the method under test
        List<DocumentChunkWithEmbedding> result = documentVectorStorage.generateEmbeddingsAndPersist(projectPath, projectId);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify that the dependencies were called with the expected arguments
        verify(documentProcessingService).processProjectToChunks(projectPath);
        verify(embeddingService, never()).generateEmbeddings(anyList());
        verify(persistenceService, never()).saveChunks(anyString(), anyList());
    }

    @Test
    void testGetDocumentChunksFromProjectWithEmbedding() {
        // Create test data
        String projectId = "test-project";
        float[] queryEmbedding = new float[1536];
        int limit = 10;

        // Create test document chunk entities
        DocumentChunkEntity entity1 = new DocumentChunkEntity(
                projectId,
                "test/path1.java",
                1,
                10,
                "Test content 1",
                new float[1536]
        );

        DocumentChunkEntity entity2 = new DocumentChunkEntity(
                projectId,
                "test/path2.java",
                11,
                20,
                "Test content 2",
                new float[1536]
        );

        // Now we only return entities from the specified project
        List<DocumentChunkEntity> similarEntities = Arrays.asList(entity1, entity2);

        // Mock the behavior of dependencies
        when(persistenceService.findSimilarChunkEntities(queryEmbedding, similarityThreshold, limit, projectId)).thenReturn(similarEntities);

        // Call the method under test
        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(projectId, queryEmbedding, limit);

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test content 1", result.get(0).getContent());
        assertEquals("test/path1.java", result.get(0).getFilePath());
        assertEquals(1, result.get(0).getStartLine());
        assertEquals(10, result.get(0).getEndLine());

        // Verify that the dependencies were called with the expected arguments
        verify(persistenceService).findSimilarChunkEntities(queryEmbedding, similarityThreshold, limit, projectId);
    }

    @Test
    void testGetDocumentChunksFromProjectWithEmbeddingNoResults() {
        // Create test data
        String projectId = "test-project";
        float[] queryEmbedding = new float[1536];
        int limit = 10;

        // Mock the behavior of dependencies
        when(persistenceService.findSimilarChunkEntities(queryEmbedding, similarityThreshold, limit, projectId)).thenReturn(List.of());

        // Call the method under test
        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(projectId, queryEmbedding, limit);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify that the dependencies were called with the expected arguments
        verify(persistenceService).findSimilarChunkEntities(queryEmbedding, similarityThreshold, limit, projectId);
    }

    @Test
    void testGetDocumentChunksFromProjectWithMessageQuery() {
        // Create test data
        String projectId = "test-project";
        String messageQuery = "test query";
        float[] generatedEmbedding = new float[1536];
        int limit = 10;

        // Create test document chunk entities
        DocumentChunkEntity entity1 = new DocumentChunkEntity(
                projectId,
                "test/path1.java",
                1,
                10,
                "Test content 1",
                new float[1536]
        );

        DocumentChunkEntity entity2 = new DocumentChunkEntity(
                projectId,
                "test/path2.java",
                11,
                20,
                "Test content 2",
                new float[1536]
        );

        // Now we only return entities from the specified project
        List<DocumentChunkEntity> similarEntities = Arrays.asList(entity1, entity2);

        // Mock the behavior of dependencies
        when(embeddingService.generateEmbedding(messageQuery)).thenReturn(generatedEmbedding);
        when(persistenceService.findSimilarChunkEntities(generatedEmbedding, similarityThreshold, limit, projectId)).thenReturn(similarEntities);

        // Call the method under test
        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(projectId, messageQuery, limit);

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test content 1", result.get(0).getContent());
        assertEquals("test/path1.java", result.get(0).getFilePath());
        assertEquals(1, result.get(0).getStartLine());
        assertEquals(10, result.get(0).getEndLine());

        // Verify that the dependencies were called with the expected arguments
        verify(embeddingService).generateEmbedding(messageQuery);
        verify(persistenceService).findSimilarChunkEntities(generatedEmbedding, similarityThreshold, limit, projectId);
    }

    @Test
    void testGetDocumentChunksFromProjectWithMessageQueryNoResults() {
        // Create test data
        String projectId = "test-project";
        String messageQuery = "test query";
        float[] generatedEmbedding = new float[1536];
        int limit = 10;

        // Mock the behavior of dependencies
        when(embeddingService.generateEmbedding(messageQuery)).thenReturn(generatedEmbedding);
        when(persistenceService.findSimilarChunkEntities(generatedEmbedding, similarityThreshold, limit, projectId)).thenReturn(List.of());

        // Call the method under test
        List<DocumentChunk> result = documentVectorStorage.getDocumentChunksFromProject(projectId, messageQuery, limit);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify that the dependencies were called with the expected arguments
        verify(embeddingService).generateEmbedding(messageQuery);
        verify(persistenceService).findSimilarChunkEntities(generatedEmbedding, similarityThreshold, limit, projectId);
    }
}
