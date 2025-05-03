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

    /**
     * Usuwa wszystkie chunki dla danego repozytorium.
     * @param projectId Identyfikator projektu.
     * @return Liczba usuniętych rekordów.
     */
    @Modifying // Wymagane dla zapytań modyfikujących (DELETE, UPDATE)
    UUID deleteByProjectId(String projectId);

    /**
     * Znajduje chunki najbardziej podobne do podanego wektora embeddingu.
     * Używa operatora <-> (kosinusowa odległość wektorowa) z pgvector.
     * UWAGA: Ta metoda jest tu dla przykładu natywnego zapytania.
     * W praktyce często używa się abstrakcji VectorStore ze Spring AI.
     *
     * @param queryEmbedding Wektor embeddingu zapytania.
     * @param limit Maksymalna liczba wyników.
     * @return Lista podobnych chunków.
     */
    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding <=> :queryEmbedding \\:\\:vector LIMIT :limit", nativeQuery = true)
    List<DocumentChunkEntity> findSimilarChunks(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit
    );
}