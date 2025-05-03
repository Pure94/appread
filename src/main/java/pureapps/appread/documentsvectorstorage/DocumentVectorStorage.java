package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentVectorStorage {

    private final PersistenceService persistenceService;
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingService embeddingService;


    public List<DocumentChunkWithEmbedding> getDocumentChunksForProject(String projectId, String queryEmbedding, int limit) {
        return;
    }

}
