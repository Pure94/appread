package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
class PersistenceService {

    private final DocumentChunkRepository chunkRepository;

    @Transactional
    void saveChunks(String projectId, List<DocumentChunkWithEmbedding> chunksWithEmbeddings) {
        if (chunksWithEmbeddings == null || chunksWithEmbeddings.isEmpty()) {
            log.warn("No chunks provided to save.");
            return;
        }

        log.info("Saving {} document chunks with embeddings to the database", chunksWithEmbeddings.size());

        try {
            List<DocumentChunkEntity> entities = chunksWithEmbeddings.stream()
                    .map(chunk -> new DocumentChunkEntity(
                            projectId,
                            chunk.getFilePath(),
                            chunk.getStartLine(),
                            chunk.getEndLine(),
                            chunk.getContent(),
                            chunk.getFileChecksum(),
                            chunk.getEmbedding()
                    ))
                    .collect(Collectors.toList());

            chunkRepository.saveAllWithVectorCast(entities);
            log.info("Successfully saved {} document chunks to the database.", entities.size());

        } catch (Exception e) {
            log.error("Error saving document chunks to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save chunks to database", e);
        }
    }


    @Transactional
    void deleteChunksForRepository(String projectId) {
        log.info("Deleting all document chunks for project: {}", projectId);
        try {
            int deletedCount = chunkRepository.deleteByProjectId(projectId);
            log.info("Deleted {} chunks for project: {}", deletedCount, projectId);
        } catch (Exception e) {
            log.error("Error deleting chunks for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete chunks for project: " + projectId, e);
        }
    }

    List<DocumentChunkEntity> findSimilarChunkEntities(float[] queryEmbedding, float similarityThreshold, int limit, String projectId) {
        log.debug("Finding {} similar chunk entities with a similarity threshold of {} for project {}", limit, similarityThreshold, projectId);
        try {
            List<DocumentChunkEntity> similarEntities = chunkRepository.findSimilarChunks(queryEmbedding, similarityThreshold, limit, projectId);
            log.info("Found {} similar chunk entities for project {}", similarEntities.size(), projectId);
            return similarEntities;
        } catch (Exception e) {
            log.error("Error finding similar chunk entities for project {}: {}", projectId, e.getMessage(), e);
            return List.of();
        }
    }

    @Transactional
    void deleteChunksForFile(String projectId, String filePath) {
        log.info("Deleting chunks for file: {} in project: {}", filePath, projectId);
        try {
            int deletedCount = chunkRepository.deleteByProjectIdAndFilePath(projectId, filePath);
            log.info("Deleted {} chunks for file: {} in project: {}", deletedCount, filePath, projectId);
        } catch (Exception e) {
            log.error("Error deleting chunks for file {} in project {}: {}", filePath, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete chunks for file: " + filePath, e);
        }
    }

    @Transactional
    void deleteChunksByChecksum(String projectId, String fileChecksum) {
        log.info("Deleting chunks with checksum: {} in project: {}", fileChecksum, projectId);
        try {
            int deletedCount = chunkRepository.deleteByProjectIdAndFileChecksum(projectId, fileChecksum);
            log.info("Deleted {} chunks with checksum: {} in project: {}", deletedCount, fileChecksum, projectId);
        } catch (Exception e) {
            log.error("Error deleting chunks with checksum {} in project {}: {}", fileChecksum, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete chunks with checksum: " + fileChecksum, e);
        }
    }
}
