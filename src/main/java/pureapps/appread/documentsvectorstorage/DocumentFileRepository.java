package pureapps.appread.documentsvectorstorage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface DocumentFileRepository extends JpaRepository<DocumentFileEntity, UUID> {

    Optional<DocumentFileEntity> findByProjectIdAndFilePath(String projectId, String filePath);

    List<DocumentFileEntity> findByProjectId(String projectId);

    @Modifying
    int deleteByProjectId(String projectId);

    @Modifying
    @Query("DELETE FROM DocumentFileEntity df WHERE df.projectId = :projectId AND df.filePath = :filePath")
    int deleteByProjectIdAndFilePath(@Param("projectId") String projectId, @Param("filePath") String filePath);

    @Query("SELECT df.filePath FROM DocumentFileEntity df WHERE df.projectId = :projectId")
    List<String> findFilePathsByProjectId(@Param("projectId") String projectId);
}