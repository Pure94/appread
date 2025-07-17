package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
class FileChecksumService {

    private final DocumentFileRepository documentFileRepository;

    /**
     * Calculate SHA-256 checksum for a file
     */
    public String calculateChecksum(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if file exists in database and if checksum matches
     */
    public FileStatus checkFileStatus(String projectId, String filePath, Path actualFilePath) throws IOException {
        Optional<DocumentFileEntity> existingFile = documentFileRepository.findByProjectIdAndFilePath(projectId, filePath);
        
        if (existingFile.isEmpty()) {
            return FileStatus.NEW;
        }

        String currentChecksum = calculateChecksum(actualFilePath);
        DocumentFileEntity fileEntity = existingFile.get();
        
        if (currentChecksum.equals(fileEntity.getChecksum())) {
            return FileStatus.UNCHANGED;
        } else {
            return FileStatus.MODIFIED;
        }
    }

    /**
     * Save or update file metadata
     */
    public DocumentFileEntity saveFileMetadata(String projectId, String filePath, Path actualFilePath) throws IOException {
        String checksum = calculateChecksum(actualFilePath);
        long fileSize = Files.size(actualFilePath);
        FileTime lastModifiedTime = Files.getLastModifiedTime(actualFilePath);
        OffsetDateTime lastModified = OffsetDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneOffset.UTC);

        Optional<DocumentFileEntity> existingFile = documentFileRepository.findByProjectIdAndFilePath(projectId, filePath);
        
        DocumentFileEntity fileEntity;
        if (existingFile.isPresent()) {
            fileEntity = existingFile.get();
            fileEntity.setChecksum(checksum);
            fileEntity.setFileSize(fileSize);
            fileEntity.setLastModified(lastModified);
        } else {
            fileEntity = new DocumentFileEntity(projectId, filePath, checksum, fileSize, lastModified);
        }

        return documentFileRepository.save(fileEntity);
    }

    /**
     * Get existing file checksum
     */
    public Optional<String> getFileChecksum(String projectId, String filePath) {
        return documentFileRepository.findByProjectIdAndFilePath(projectId, filePath)
                .map(DocumentFileEntity::getChecksum);
    }

    /**
     * Delete file metadata
     */
    public void deleteFileMetadata(String projectId, String filePath) {
        documentFileRepository.deleteByProjectIdAndFilePath(projectId, filePath);
    }

    /**
     * Delete all file metadata for a project
     */
    public void deleteAllFileMetadata(String projectId) {
        documentFileRepository.deleteByProjectId(projectId);
    }

    public enum FileStatus {
        NEW,        // File doesn't exist in database
        UNCHANGED,  // File exists and checksum matches
        MODIFIED    // File exists but checksum is different
    }
}