package pureapps.appread.documentsvectorstorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class EmbeddingServiceTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingModel = Mockito.mock(EmbeddingModel.class);

        float[] mockEmbedding = new float[1536];
        Arrays.fill(mockEmbedding, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(mockEmbedding);

        embeddingService = new EmbeddingService(embeddingModel);
    }

    @Test
    void testGenerateEmbedding() {
        // Create a test document chunk
        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent("Test content");
        chunk.setFilePath("test/path.java");
        chunk.setStartLine(1);
        chunk.setEndLine(10);

        // Generate embedding
        DocumentChunkWithEmbedding result = embeddingService.generateEmbedding(chunk);

        // Verify the result
        assertNotNull(result);
        assertEquals("Test content", result.getContent());
        assertEquals("test/path.java", result.getFilePath());
        assertEquals(1, result.getStartLine());
        assertEquals(10, result.getEndLine());
        assertNotNull(result.getEmbedding());
        assertEquals(1536, result.getEmbedding().length);
    }

    @Test
    void testGenerateEmbeddings() {
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

        List<DocumentChunkWithEmbedding> results = embeddingService.generateEmbeddings(chunks);

        assertNotNull(results);
        assertEquals(2, results.size());

        assertEquals("Test content 1", results.get(0).getContent());
        assertEquals("test/path1.java", results.get(0).getFilePath());
        assertEquals(1, results.get(0).getStartLine());
        assertEquals(10, results.get(0).getEndLine());
        assertNotNull(results.get(0).getEmbedding());
        assertEquals(1536, results.get(0).getEmbedding().length);

        assertEquals("Test content 2", results.get(1).getContent());
        assertEquals("test/path2.java", results.get(1).getFilePath());
        assertEquals(11, results.get(1).getStartLine());
        assertEquals(20, results.get(1).getEndLine());
        assertNotNull(results.get(1).getEmbedding());
        assertEquals(1536, results.get(1).getEmbedding().length);
    }
}
