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
class FileChecksumServiceTest {

    @Autowired
    private FileChecksumService fileChecksumService;

    @TempDir
    Path tempDir;

    @Test
    void shouldCalculateChecksumForFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content);

        // When
        String checksum = fileChecksumService.calculateChecksum(testFile);

        // Then
        assertNotNull(checksum);
        assertEquals(64, checksum.length()); // SHA-256 produces 64 character hex string
    }

    @Test
    void shouldReturnSameChecksumForSameContent() throws IOException {
        // Given
        Path testFile1 = tempDir.resolve("test1.txt");
        Path testFile2 = tempDir.resolve("test2.txt");
        String content = "Hello, World!";
        Files.writeString(testFile1, content);
        Files.writeString(testFile2, content);

        // When
        String checksum1 = fileChecksumService.calculateChecksum(testFile1);
        String checksum2 = fileChecksumService.calculateChecksum(testFile2);

        // Then
        assertEquals(checksum1, checksum2);
    }

    @Test
    void shouldReturnDifferentChecksumForDifferentContent() throws IOException {
        // Given
        Path testFile1 = tempDir.resolve("test1.txt");
        Path testFile2 = tempDir.resolve("test2.txt");
        Files.writeString(testFile1, "Hello, World!");
        Files.writeString(testFile2, "Hello, Universe!");

        // When
        String checksum1 = fileChecksumService.calculateChecksum(testFile1);
        String checksum2 = fileChecksumService.calculateChecksum(testFile2);

        // Then
        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void shouldDetectNewFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");
        String projectId = "test-project";
        String filePath = "test.txt";

        // When
        FileChecksumService.FileStatus status = fileChecksumService.checkFileStatus(projectId, filePath, testFile);

        // Then
        assertEquals(FileChecksumService.FileStatus.NEW, status);
    }

    @Test
    void shouldDetectUnchangedFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content);
        String projectId = "test-project";
        String filePath = "test.txt";

        // Save file metadata first
        fileChecksumService.saveFileMetadata(projectId, filePath, testFile);

        // When
        FileChecksumService.FileStatus status = fileChecksumService.checkFileStatus(projectId, filePath, testFile);

        // Then
        assertEquals(FileChecksumService.FileStatus.UNCHANGED, status);
    }

    @Test
    void shouldDetectModifiedFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String originalContent = "Hello, World!";
        Files.writeString(testFile, originalContent);
        String projectId = "test-project";
        String filePath = "test.txt";

        // Save file metadata first
        fileChecksumService.saveFileMetadata(projectId, filePath, testFile);

        // Modify the file
        String modifiedContent = "Hello, Universe!";
        Files.writeString(testFile, modifiedContent);

        // When
        FileChecksumService.FileStatus status = fileChecksumService.checkFileStatus(projectId, filePath, testFile);

        // Then
        assertEquals(FileChecksumService.FileStatus.MODIFIED, status);
    }
}