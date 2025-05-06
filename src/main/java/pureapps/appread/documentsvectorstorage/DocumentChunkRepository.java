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
    UUID deleteByProjectId(String projectId);

    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding <=> :queryEmbedding \\:\\:vector LIMIT :limit", nativeQuery = true)
    List<DocumentChunkEntity> findSimilarChunks(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit
    );
}