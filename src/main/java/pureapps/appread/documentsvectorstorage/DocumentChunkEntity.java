package pureapps.appread.documentsvectorstorage;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "document_chunks_project_id_idx", columnList = "project_id"),
        @Index(name = "document_chunks_file_path_idx", columnList = "file_path")
})
@Getter
@Setter
@NoArgsConstructor
class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "start_line", nullable = false)
    private int startLine;

    @Column(name = "end_line", nullable = false)
    private int endLine;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private float[] embedding;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public DocumentChunkEntity(String projectId, String filePath, int startLine, int endLine, String content, float[] embedding) {
        this.projectId = projectId;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        setEmbeddingFromArray(embedding);
    }

    public void setEmbeddingFromArray(float[] embeddingArray) {
        this.embedding = embeddingArray;
    }
}
