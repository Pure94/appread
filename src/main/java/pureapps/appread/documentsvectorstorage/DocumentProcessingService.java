
package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
class DocumentProcessingService {

    private final FileChecksumService fileChecksumService;

    @Value("${app.document.chunk-size:50}")
    private int chunkSize;

    @Value("${app.document.overlap-percentage:10}")
    private int overlapPercentage;

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".java", ".kt", ".js", ".ts", ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".cs", ".php", ".html", ".css", ".md", ".txt", ".json", ".xml", ".yaml", ".yml"
    );

    private static final List<String> IGNORED_PATTERNS = List.of(
            ".git", ".svn", ".hg", "CVS",
            "node_modules", "bower_components", "vendor", "Pods", "packages",
            "build", "dist", "target", "out", "bin", "obj", "gen",
            ".idea", ".vscode", ".project", ".classpath", ".settings", ".DS_Store",
            "*.iml", "*.suo", "*.user", "*.tmproj", "*.sublime-project", "*.sublime-workspace",
            "logs", "tmp", "temp", ".cache", ".npm", ".yarn", ".gradle", ".mvn",
            "*.log", "*.swp", "*~",
            "__pycache__", ".pytest_cache", ".tox", ".venv", "venv", "env", "*.pyc",
            "coverage", ".nyc_output", ".aider",
            "package-lock.json",  "pnpm-lock.yaml", "*.lock", "go.sum"
    );

    List<DocumentChunk> processProjectToChunks(Path projectPath) throws IOException {
        List<DocumentChunk> allChunks = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(projectPath)) {
            List<Path> filesToProcess = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isFileSupported)
                    .filter(this::isNotIgnored)
                    .toList();

            log.info("Found {} files to process", filesToProcess.size());
            for (Path filePath : filesToProcess) {
                try {
                    List<DocumentChunk> fileChunks = processFile(projectPath, filePath);
                    allChunks.addAll(fileChunks);
                } catch (Exception e) {
                    log.error("Error processing file {}: {}", filePath, e.getMessage());
                }
            }
        }

        return allChunks;
    }

    /**
     * Process project files with checksum-based change detection
     */
    ProcessingResult processProjectToChunksWithChecksumCheck(Path projectPath, String projectId) throws IOException {
        List<DocumentChunk> newChunks = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> unchangedFiles = new ArrayList<>();
        List<String> newFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectPath)) {
            List<Path> filesToProcess = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isFileSupported)
                    .filter(this::isNotIgnored)
                    .toList();

            log.info("Found {} files to process with checksum check", filesToProcess.size());
            
            for (Path filePath : filesToProcess) {
                try {
                    String relativePath = projectPath.relativize(filePath).toString();
                    FileChecksumService.FileStatus status = fileChecksumService.checkFileStatus(projectId, relativePath, filePath);
                    
                    switch (status) {
                        case UNCHANGED:
                            log.debug("File unchanged, skipping: {}", relativePath);
                            unchangedFiles.add(relativePath);
                            break;
                            
                        case NEW:
                            log.info("Processing new file: {}", relativePath);
                            List<DocumentChunk> fileChunks = processFileWithChecksum(projectPath, filePath);
                            newChunks.addAll(fileChunks);
                            newFiles.add(relativePath);
                            break;
                            
                        case MODIFIED:
                            log.info("Processing modified file: {}", relativePath);
                            List<DocumentChunk> modifiedFileChunks = processFileWithChecksum(projectPath, filePath);
                            newChunks.addAll(modifiedFileChunks);
                            modifiedFiles.add(relativePath);
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing file {}: {}", filePath, e.getMessage());
                }
            }
        }

        return new ProcessingResult(newChunks, newFiles, modifiedFiles, unchangedFiles);
    }

    private List<DocumentChunk> processFileWithChecksum(Path projectPath, Path filePath) throws IOException {
        String relativePath = projectPath.relativize(filePath).toString();
        String content = Files.readString(filePath);
        String checksum = fileChecksumService.calculateChecksum(filePath);

        log.info("Processing file: {} with checksum: {}", relativePath, checksum);
        List<DocumentChunk> chunks = splitIntoChunksWithChecksum(content, relativePath, checksum);

        log.info("Created {} chunks from file {}", chunks.size(), relativePath);

        return chunks;
    }

    private List<DocumentChunk> splitIntoChunksWithChecksum(String content, String path, String checksum) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        int overlapSize = Math.max(1, (int) Math.ceil(chunkSize * overlapPercentage / 100.0));
        int totalLines = lines.length;

        log.debug("Splitting file {} into chunks with checksum {}. Chunk size: {}, Overlap: {}% ({} lines)",
                path, checksum, chunkSize, overlapPercentage, overlapSize);

        if (totalLines <= chunkSize) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(content);
            chunk.setFilePath(path);
            chunk.setStartLine(1);
            chunk.setEndLine(totalLines);
            chunk.setFileChecksum(checksum);
            chunks.add(chunk);
            return chunks;
        }

        for (int i = 0; i < totalLines; i += (chunkSize - overlapSize)) {
            int end = Math.min(i + chunkSize, totalLines);

            if (totalLines - end < overlapSize && end < totalLines) {
                end = totalLines;
            }

            StringBuilder chunkContent = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunkContent.append(lines[j]).append("\n");
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(chunkContent.toString());
            chunk.setFilePath(path);
            chunk.setStartLine(i + 1);
            chunk.setEndLine(end);
            chunk.setFileChecksum(checksum);

            chunks.add(chunk);

            if (end == totalLines) {
                break;
            }
        }

        return chunks;
    }

    private List<DocumentChunk> processFile(Path projectPath, Path filePath) throws IOException {
        String relativePath = projectPath.relativize(filePath).toString();
        String content = Files.readString(filePath);

        log.info("Processing file: {}", relativePath);
        List<DocumentChunk> chunks = splitIntoChunks(content, relativePath);

        log.info("Created {} chunks from file {}", chunks.size(), relativePath);

        return chunks;
    }

    private List<DocumentChunk> splitIntoChunks(String content, String path) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        int overlapSize = Math.max(1, (int) Math.ceil(chunkSize * overlapPercentage / 100.0));
        int totalLines = lines.length;

        log.debug("Splitting file {} into chunks. Chunk size: {}, Overlap: {}% ({} lines)",
                path, chunkSize, overlapPercentage, overlapSize);

        if (totalLines <= chunkSize) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(content);
            chunk.setFilePath(path);
            chunk.setStartLine(1);
            chunk.setEndLine(totalLines);
            chunks.add(chunk);
            return chunks;
        }

        for (int i = 0; i < totalLines; i += (chunkSize - overlapSize)) {
            int end = Math.min(i + chunkSize, totalLines);

            if (totalLines - end < overlapSize && end < totalLines) {
                end = totalLines;
            }

            StringBuilder chunkContent = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunkContent.append(lines[j]).append("\n");
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(chunkContent.toString());
            chunk.setFilePath(path);
            chunk.setStartLine(i + 1);
            chunk.setEndLine(end);

            chunks.add(chunk);

            if (end == totalLines) {
                break;
            }
        }

        return chunks;
    }

    private boolean isFileSupported(Path path) {
        String fileName = path.toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private boolean isNotIgnored(Path path) {
        String pathStr = path.toString();
        return IGNORED_PATTERNS.stream().noneMatch(pathStr::contains);
    }

}
