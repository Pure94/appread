package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
class DocumentVectorStorage {

    private final PersistenceService persistenceService;
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingService embeddingService;


    public List<DocumentChunkWithEmbedding> generateEmbeddingsAndPersist(Path projectPath) {
        try {
            List<DocumentChunk> chunks = documentProcessingService.processProjectToChunks(projectPath);

            if (chunks.isEmpty()) {
                return List.of();
            }

            List<DocumentChunkWithEmbedding> chunksWithEmbeddings = embeddingService.generateEmbeddings(chunks);
            saveDocumentChunks(chunksWithEmbeddings);

            return chunksWithEmbeddings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embeddings and persist for project: " + projectPath, e);
        }
    }

    public List<DocumentChunkWithEmbedding> getDocumentChunksForProject(String projectId, String messageQuery, int limit) {

        return getDocumentChunksForProject(projectId, embeddingService.generateEmbedding(messageQuery), limit);
    }


    public List<DocumentChunkWithEmbedding> getDocumentChunksForProject(String projectId, float[] queryEmbedding, int limit) {
        try {
            List<DocumentChunkEntity> similarEntities = persistenceService.findSimilarChunkEntities(queryEmbedding, limit * 2);

            List<DocumentChunkEntity> filteredEntities = similarEntities.stream()
                    .filter(entity -> entity.getProjectId().equals(projectId))
                    .limit(limit)
                    .toList();

            return DocumentChunkMapper.toDTOs(filteredEntities);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document chunks for project: " + projectId, e);
        }
    }

    private void saveDocumentChunks(List<DocumentChunkWithEmbedding> chunksWithEmbeddings) {
        persistenceService.saveChunks(chunksWithEmbeddings);
    }
}
