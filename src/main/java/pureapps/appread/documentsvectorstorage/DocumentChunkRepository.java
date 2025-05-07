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

    List<DocumentChunkEntity> findAll(Specification<DocumentChunkEntity> spec);

    @Modifying
    @Query(value = "INSERT INTO document_chunks (uuid, project_id, file_path, start_line, end_line, content, embedding, created_at) " +
            "VALUES (gen_random_uuid(), :#{#entity.projectId}, :#{#entity.filePath}, :#{#entity.startLine}, :#{#entity.endLine}, " +
            ":#{#entity.content}, CAST(:#{#entity.embedding} AS vector), CURRENT_TIMESTAMP)", nativeQuery = true)
    void saveWithVectorCast(@Param("entity") DocumentChunkEntity entity);

    default void saveAllWithVectorCast(List<DocumentChunkEntity> entities) {
        entities.forEach(this::saveWithVectorCast);
    }
}
