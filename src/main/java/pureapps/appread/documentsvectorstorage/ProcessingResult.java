package pureapps.appread.documentsvectorstorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;

import java.util.List;

@Getter
@AllArgsConstructor
class ProcessingResult {
    private final List<DocumentChunk> newChunks;
    private final List<String> newFiles;
    private final List<String> modifiedFiles;
    private final List<String> unchangedFiles;

    public boolean hasChanges() {
        return !newFiles.isEmpty() || !modifiedFiles.isEmpty();
    }

    public int getTotalProcessedFiles() {
        return newFiles.size() + modifiedFiles.size() + unchangedFiles.size();
    }
}