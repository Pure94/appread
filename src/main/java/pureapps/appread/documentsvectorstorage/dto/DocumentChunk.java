package pureapps.appread.documentsvectorstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DocumentChunk {

    private String content;
    private String filePath;
    private int startLine;
    private int endLine;
    private String fileChecksum;

}
