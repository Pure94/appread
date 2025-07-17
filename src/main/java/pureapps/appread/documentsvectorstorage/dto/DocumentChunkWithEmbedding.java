package pureapps.appread.documentsvectorstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentChunkWithEmbedding {
    private String content;
    private String filePath;
    private int startLine;
    private int endLine;
    private String fileChecksum;
    private float[] embedding;
}