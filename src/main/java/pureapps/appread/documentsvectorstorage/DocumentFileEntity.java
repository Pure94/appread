package pureapps.appread.documentsvectorstorage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_files", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "file_path"}),
       indexes = {
        @Index(name = "document_files_project_id_idx", columnList = "project_id"),
        @Index(name = "document_files_file_path_idx", columnList = "file_path"),
        @Index(name = "document_files_checksum_idx", columnList = "checksum")
})
@Getter
@Setter
@NoArgsConstructor
class DocumentFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID uuid;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "last_modified", nullable = false)
    private OffsetDateTime lastModified;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public DocumentFileEntity(String projectId, String filePath, String checksum, long fileSize, OffsetDateTime lastModified) {
        this.projectId = projectId;
        this.filePath = filePath;
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
    }
}