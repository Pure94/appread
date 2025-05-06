
package pureapps.appread.documentsvectorstorage;

import lombok.experimental.UtilityClass;
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
    public DocumentChunkWithEmbedding toDTO(DocumentChunkEntity entity) {
        DocumentChunkWithEmbedding dto = new DocumentChunkWithEmbedding();
        dto.setContent(entity.getContent());
        dto.setFilePath(entity.getFilePath());
        dto.setStartLine(entity.getStartLine());
        dto.setEndLine(entity.getEndLine());
        dto.setEmbedding(entity.getEmbedding());
        return dto;
    }

    /**
     * Maps a list of DocumentChunkEntity objects to DocumentChunkWithEmbedding objects.
     *
     * @param entities The source entities
     * @return A list of DocumentChunkWithEmbedding objects
     */
    public List<DocumentChunkWithEmbedding> toDTOs(List<DocumentChunkEntity> entities) {
        return entities.stream()
                .map(DocumentChunkMapper::toDTO)
                .collect(Collectors.toList());
    }

}
