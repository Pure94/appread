package pureapps.appread.documentsvectorstorage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    @Modifying
    int deleteByProjectId(String projectId);

    @Query(value = "SELECT uuid, project_id, file_path, start_line, end_line, content, NULL as embedding, created_at " +
                   "FROM document_chunks " +
                   "WHERE (embedding <=> CAST(:queryEmbedding AS vector)) <= :similarityThreshold " +
                   "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<DocumentChunkEntity> findSimilarChunks(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("similarityThreshold") float similarityThreshold,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = "INSERT INTO document_chunks (uuid, project_id, file_path, start_line, end_line, content, embedding, created_at) " +
            "VALUES (gen_random_uuid(), :#{#entity.projectId}, :#{#entity.filePath}, :#{#entity.startLine}, :#{#entity.endLine}, " +
            ":#{#entity.content}, CAST(:#{#entity.embedding} AS vector), CURRENT_TIMESTAMP)", nativeQuery = true)
    void saveWithVectorCast(@Param("entity") DocumentChunkEntity entity);

    default void saveAllWithVectorCast(List<DocumentChunkEntity> entities) {
        entities.forEach(this::saveWithVectorCast);
    }
}
