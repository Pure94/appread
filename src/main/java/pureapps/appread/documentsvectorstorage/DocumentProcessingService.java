package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for processing documents from a repository.
 * This includes reading files, splitting them into chunks, and preparing them for embedding.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class DocumentProcessingService {

    // File extensions to process
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".java", ".kt", ".js", ".ts", ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".cs", ".php", ".html", ".css", ".md", ".txt", ".json", ".xml", ".yaml", ".yml"
    );

    // Files to ignore
    private static final List<String> IGNORED_PATTERNS = List.of(
            // Kontrola wersji
            ".git", ".svn", ".hg", "CVS",
            // Zależności i Pakiety
            "node_modules", "bower_components", "vendor", "Pods", "packages",
            // Wyniki kompilacji / Build
            "build", "dist", "target", "out", "bin", "obj", "gen",
            // IDE / Edytor
            ".idea", ".vscode", ".project", ".classpath", ".settings", ".DS_Store",
            "*.iml", "*.suo", "*.user", "*.tmproj", "*.sublime-project", "*.sublime-workspace",
            // Cache / Logi / Temp
            "logs", "tmp", "temp", ".cache", ".npm", ".yarn", ".gradle", ".mvn",
            "*.log", "*.swp", "*~",
            // Python
            "__pycache__", ".pytest_cache", ".tox", ".venv", "venv", "env", "*.pyc",
            // Testy / Pokrycie
            "coverage", ".nyc_output"
            // Można dodać więcej specyficznych dla projektu wzorców
    );

    /**
     * Process all files in a repository.
     *
     * @param projectPath The path to the project
     * @return A list of document chunks ready for embedding
     * @throws IOException If there's an error reading files
     */
    List<DocumentChunk> processProjectToChunks(Path projectPath) throws IOException {
        List<DocumentChunk> allChunks = new ArrayList<>();

        // Find all files in the repository
        try (Stream<Path> paths = Files.walk(projectPath)) {
            List<Path> filesToProcess = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isFileSupported)
                    .filter(this::isNotIgnored)
                    .collect(Collectors.toList());

            log.info("Found {} files to process", filesToProcess.size());

            // Process each file
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
     * Process a single file.
     *
     * @param projectPath The path to the repository
     * @param filePath    The path to the file
     * @return A list of document chunks from the file
     * @throws IOException If there's an error reading the file
     */
    private List<DocumentChunk> processFile(Path projectPath, Path filePath) throws IOException {
        String relativePath = projectPath.relativize(filePath).toString();
        String content = Files.readString(filePath);

        log.info("Processing file: {}", relativePath);

        // Split the content into chunks
        List<DocumentChunk> chunks = splitIntoChunks(content, relativePath);

        log.info("Created {} chunks from file {}", chunks.size(), relativePath);

        return chunks;
    }

    /**
     * Split a document into chunks.
     *
     * @param content The content of the document
     * @param path    The path to the file
     * @return A list of document chunks
     */
    private List<DocumentChunk> splitIntoChunks(String content, String path) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // Simple chunking by lines (this would be replaced with a more sophisticated approach)
        String[] lines = content.split("\n");

        int chunkSize = 50; // Number of lines per chunk
        int totalLines = lines.length;

        for (int i = 0; i < totalLines; i += chunkSize) {
            int end = Math.min(i + chunkSize, totalLines);

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
        }

        return chunks;
    }

    /**
     * Check if a file is supported based on its extension.
     *
     * @param path The path to the file
     * @return True if the file is supported, false otherwise
     */
    private boolean isFileSupported(Path path) {
        String fileName = path.toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * Check if a file should be ignored.
     *
     * @param path The path to the file
     * @return True if the file should be ignored, false otherwise
     */
    private boolean isNotIgnored(Path path) {
        String pathStr = path.toString();
        return IGNORED_PATTERNS.stream().noneMatch(pathStr::contains);
    }

    /**
     * Document chunk class representing a portion of a document.
     */
    public static class DocumentChunk {
        private String content;
        private String filePath;
        private int startLine;
        private int endLine;

        // Getters and setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }
    }
}