package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentVectorStorage {

    private final PersistenceService persistenceService;
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingService embeddingService;
    private final FileChecksumService fileChecksumService;

    @Value("${app.document.search.similarity-threshold:0.7}")
    private float similarityThreshold;


    public List<DocumentChunkWithEmbedding> generateEmbeddingsAndPersist(Path projectPath, String projectId) {
        try {
            List<DocumentChunk> chunks = documentProcessingService.processProjectToChunks(projectPath);

            if (chunks.isEmpty()) {
                return List.of();
            }

            List<DocumentChunkWithEmbedding> chunksWithEmbeddings = embeddingService.generateEmbeddings(chunks);
            saveDocumentChunks(projectId, chunksWithEmbeddings);

            return chunksWithEmbeddings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embeddings and persist for project: " + projectPath, e);
        }
    }

    /**
     * Generate embeddings and persist with checksum-based change detection.
     * Only processes new or modified files, skips unchanged files.
     */
    public ProcessingResult generateEmbeddingsAndPersistWithChecksumCheck(Path projectPath, String projectId) {
        try {
            ProcessingResult processingResult = documentProcessingService.processProjectToChunksWithChecksumCheck(projectPath, projectId);

            if (!processingResult.hasChanges()) {
                return processingResult;
            }

            // Handle modified files - delete old chunks first
            for (String modifiedFile : processingResult.getModifiedFiles()) {
                persistenceService.deleteChunksForFile(projectId, modifiedFile);
            }

            // Generate embeddings for new and modified files
            if (!processingResult.getNewChunks().isEmpty()) {
                List<DocumentChunkWithEmbedding> chunksWithEmbeddings = embeddingService.generateEmbeddings(processingResult.getNewChunks());
                saveDocumentChunks(projectId, chunksWithEmbeddings);

                // Update file metadata for all processed files
                updateFileMetadata(projectPath, projectId, processingResult);
            }

            return processingResult;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embeddings and persist with checksum check for project: " + projectPath, e);
        }
    }

    private void updateFileMetadata(Path projectPath, String projectId, ProcessingResult processingResult) {
        try {
            // Update metadata for new files
            for (String newFile : processingResult.getNewFiles()) {
                Path filePath = projectPath.resolve(newFile);
                fileChecksumService.saveFileMetadata(projectId, newFile, filePath);
            }

            // Update metadata for modified files
            for (String modifiedFile : processingResult.getModifiedFiles()) {
                Path filePath = projectPath.resolve(modifiedFile);
                fileChecksumService.saveFileMetadata(projectId, modifiedFile, filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update file metadata", e);
        }
    }

    public List<DocumentChunk> getDocumentChunksFromProject(String projectId, String messageQuery, int limit) {

        return getDocumentChunksFromProject(projectId, embeddingService.generateEmbedding(messageQuery), limit);
    }


    public List<DocumentChunk> getDocumentChunksFromProject(String projectId, float[] queryEmbedding, int limit) {
        try {
            List<DocumentChunkEntity> similarEntities = persistenceService.findSimilarChunkEntities(queryEmbedding, similarityThreshold, limit, projectId);
            return similarEntities.stream()
                    .map(DocumentChunkMapper::toDTO)
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get document chunks for project: " + projectId, e);
            }
        }

    private void saveDocumentChunks(String projectId, List<DocumentChunkWithEmbedding> chunksWithEmbeddings) {
        persistenceService.saveChunks(projectId, chunksWithEmbeddings);
    }

}
