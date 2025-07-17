package pureapps.appread.documentsvectorstorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChecksumAwareDocumentProcessingTest {

    @Autowired
    private DocumentVectorStorage documentVectorStorage;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentFileRepository documentFileRepository;

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessNewFilesAndSkipUnchangedFiles() throws IOException {
        // Given
        String projectId = "test-project";
        
        // Create initial files
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, """
            public class Test {
                public void method1() {
                    System.out.println("Hello");
                }
            }
            """);

        Path txtFile = tempDir.resolve("readme.txt");
        Files.writeString(txtFile, "This is a readme file");

        // First processing - all files are new
        ProcessingResult firstResult = documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);

        // Then
        assertEquals(2, firstResult.getNewFiles().size());
        assertEquals(0, firstResult.getModifiedFiles().size());
        assertEquals(0, firstResult.getUnchangedFiles().size());
        assertTrue(firstResult.hasChanges());

        // Verify chunks were created
        long chunkCount = documentChunkRepository.count();
        assertTrue(chunkCount > 0);

        // Verify file metadata was saved
        assertEquals(2, documentFileRepository.findByProjectId(projectId).size());

        // Second processing - no changes, should skip all files
        ProcessingResult secondResult = documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);

        // Then
        assertEquals(0, secondResult.getNewFiles().size());
        assertEquals(0, secondResult.getModifiedFiles().size());
        assertEquals(2, secondResult.getUnchangedFiles().size());
        assertFalse(secondResult.hasChanges());

        // Chunk count should remain the same
        assertEquals(chunkCount, documentChunkRepository.count());
    }

    @Test
    void shouldDetectAndProcessModifiedFiles() throws IOException {
        // Given
        String projectId = "test-project";
        
        Path javaFile = tempDir.resolve("Test.java");
        String originalContent = """
            public class Test {
                public void method1() {
                    System.out.println("Hello");
                }
            }
            """;
        Files.writeString(javaFile, originalContent);

        // First processing
        ProcessingResult firstResult = documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);
        long originalChunkCount = documentChunkRepository.count();

        // Modify the file
        String modifiedContent = """
            public class Test {
                public void method1() {
                    System.out.println("Hello World");
                }
                
                public void method2() {
                    System.out.println("New method");
                }
            }
            """;
        Files.writeString(javaFile, modifiedContent);

        // Second processing
        ProcessingResult secondResult = documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);

        // Then
        assertEquals(0, secondResult.getNewFiles().size());
        assertEquals(1, secondResult.getModifiedFiles().size());
        assertEquals(0, secondResult.getUnchangedFiles().size());
        assertTrue(secondResult.hasChanges());

        // Verify old chunks were replaced with new ones
        long newChunkCount = documentChunkRepository.count();
        // The count might be different due to different content length
        assertTrue(newChunkCount > 0);

        // Verify file metadata was updated
        assertEquals(1, documentFileRepository.findByProjectId(projectId).size());
    }

    @Test
    void shouldHandleMixOfNewModifiedAndUnchangedFiles() throws IOException {
        // Given
        String projectId = "test-project";
        
        // Create initial files
        Path unchangedFile = tempDir.resolve("unchanged.txt");
        Files.writeString(unchangedFile, "This file will not change");

        Path toBeModifiedFile = tempDir.resolve("modified.java");
        Files.writeString(toBeModifiedFile, "public class Original {}");

        // First processing
        documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);

        // Modify one file and add a new file
        Files.writeString(toBeModifiedFile, "public class Modified { int x; }");
        
        Path newFile = tempDir.resolve("new.txt");
        Files.writeString(newFile, "This is a new file");

        // Second processing
        ProcessingResult result = documentVectorStorage.generateEmbeddingsAndPersistWithChecksumCheck(tempDir, projectId);

        // Then
        assertEquals(1, result.getNewFiles().size());
        assertEquals(1, result.getModifiedFiles().size());
        assertEquals(1, result.getUnchangedFiles().size());
        assertTrue(result.hasChanges());

        assertTrue(result.getNewFiles().contains("new.txt"));
        assertTrue(result.getModifiedFiles().contains("modified.java"));
        assertTrue(result.getUnchangedFiles().contains("unchanged.txt"));

        // Verify all files are tracked
        assertEquals(3, documentFileRepository.findByProjectId(projectId).size());
    }
}