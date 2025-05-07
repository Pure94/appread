
package pureapps.appread.documentsvectorstorage;

import lombok.experimental.UtilityClass;
import org.springframework.ai.document.Document;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between different document chunk models.
 */
@UtilityClass
class DocumentChunkMapper {

    /**
     * Maps a DocumentChunkEntity to a DocumentChunkWithEmbedding.
     *
     * @param entity The source entity
     * @return A new DocumentChunkWithEmbedding
     */
    public DocumentChunk toDTO(DocumentChunkEntity entity) {
        DocumentChunk dto = new DocumentChunk();
        dto.setContent(entity.getContent());
        dto.setFilePath(entity.getFilePath());
        dto.setStartLine(entity.getStartLine());
        dto.setEndLine(entity.getEndLine());
        return dto;
    }


}
