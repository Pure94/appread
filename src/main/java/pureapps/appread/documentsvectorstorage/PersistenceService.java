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
    void saveChunks(List<DocumentChunkWithEmbedding> chunksWithEmbeddings) {
        if (chunksWithEmbeddings == null || chunksWithEmbeddings.isEmpty()) {
            log.warn("No chunks provided to save.");
            return;
        }

        log.info("Saving {} document chunks with embeddings to the database", chunksWithEmbeddings.size());

        try {
            List<DocumentChunkEntity> entities = chunksWithEmbeddings.stream()
                    .map(chunk -> new DocumentChunkEntity(
                            "temp-project-id",
                            chunk.getFilePath(),
                            chunk.getStartLine(),
                            chunk.getEndLine(),
                            chunk.getContent(),
                            chunk.getEmbedding()
                    ))
                    .collect(Collectors.toList());

            chunkRepository.saveAll(entities);
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
            UUID deletedCount = chunkRepository.deleteByProjectId(projectId);
            log.info("Deleted {} chunks for project: {}", deletedCount, projectId);
        } catch (Exception e) {
            log.error("Error deleting chunks for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete chunks for project: " + projectId, e);
        }
    }

    List<DocumentChunkEntity> findSimilarChunkEntities(float[] queryEmbedding, int limit) {
        log.debug("Finding {} similar chunk entities using native query", limit);
        try {
            List<DocumentChunkEntity> similarEntities = chunkRepository.findSimilarChunks(queryEmbedding, limit);
            log.info("Found {} similar chunk entities", similarEntities.size());
            return similarEntities;
        } catch (Exception e) {
            log.error("Error finding similar chunk entities: {}", e.getMessage(), e);
            return List.of();
        }
    }
}