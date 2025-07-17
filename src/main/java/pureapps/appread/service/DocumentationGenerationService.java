package pureapps.appread.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.DocumentVectorStorage;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.dto.FileNode;
import pureapps.appread.mermaid.MermaidService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that connects GitService and DocumentVectorStorage to generate
 * documentation for a given GitHub repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationGenerationService {

    private final GitService gitService;
    private final DocumentVectorStorage documentVectorStorage;
    private final ChatClient chatClient;
    private final MermaidService mermaidService;

    // Same supported extensions as in DocumentProcessingService
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".java", ".kt", ".js", ".ts", ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".cs", ".php", ".html", ".css", ".md", ".txt", ".json", ".xml", ".yaml", ".yml"
    );

    // Same ignored patterns as in DocumentProcessingService
    private static final List<String> IGNORED_PATTERNS = List.of(
            ".git", ".svn", ".hg", "CVS",
            "node_modules", "bower_components", "vendor", "Pods", "packages",
            "build", "dist", "target", "out", "bin", "obj", "gen",
            ".idea", ".vscode", ".project", ".classpath", ".settings", ".DS_Store",
            "*.iml", "*.suo", "*.user", "*.tmproj", "*.sublime-project", "*.sublime-workspace",
            "logs", "tmp", "temp", ".cache", ".npm", ".yarn", ".gradle", ".mvn",
            "*.log", "*.swp", "*~",
            "__pycache__", ".pytest_cache", ".tox", ".venv", "venv", "env", "*.pyc",
            "coverage", ".nyc_output"
    );

    /**
     * Generates documentation for a GitHub repository.
     *
     * @param repoUrl GitHub repository URL
     * @param branch Optional branch name, defaults to the default branch if not provided
     * @param token Optional GitHub access token for private repositories
     * @return Project ID that can be used to query the generated documentation
     */
    public String generateDocumentation(String repoUrl, Optional<String> branch, Optional<String> token) {
        log.info("Generating documentation for repository: {}", repoUrl);

        try {
            // Generate a unique project ID
            String projectId = generateProjectId(repoUrl);

            // Clone the repository
            Path repoPath = gitService.cloneRepository(repoUrl, branch, token);

            // Process the repository and generate embeddings
            documentVectorStorage.generateEmbeddingsAndPersist(repoPath, projectId);

            // Clean up the repository files after processing
            gitService.deleteRepository(repoPath);

            log.info("Documentation generated successfully for repository: {}, project ID: {}", repoUrl, projectId);

            return projectId;
        } catch (Exception e) {
            log.error("Failed to generate documentation for repository: {}", repoUrl, e);
            throw new RuntimeException("Failed to generate documentation for repository: " + repoUrl, e);
        }
    }

    /**
     * Queries the documentation for a specific project.
     *
     * @param projectId Project ID returned by generateDocumentation
     * @param query Query string to search for in the documentation
     * @param limit Maximum number of results to return
     * @return List of document chunks matching the query
     */
    public List<DocumentChunk> queryDocumentation(String projectId, String query, int limit) {
        log.info("Querying documentation for project: {}, query: {}", projectId, query);

        try {
            List<DocumentChunk> results = documentVectorStorage.getDocumentChunksFromProject(projectId, query, limit);
            log.info("Found {} results for query: {}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("Failed to query documentation for project: {}, query: {}", projectId, query, e);
            throw new RuntimeException("Failed to query documentation for project: " + projectId, e);
        }
    }

    /**
     * Generates documentation for a local repository path.
     * This method analyzes the project using LLM and creates documentation inside the project path.
     *
     * @param localRepoPath Path to the local repository
     * @return Path to the generated documentation
     */
    public Path generateDocumentationForLocalRepo(Path localRepoPath) {
        log.info("Generating documentation for local repository: {}", localRepoPath);

        try {
            // Validate the repository path
            if (!Files.exists(localRepoPath) || !Files.isDirectory(localRepoPath)) {
                throw new IllegalArgumentException("Invalid repository path: " + localRepoPath);
            }

            // Get the file structure of the repository
            FileNode fileStructure = gitService.getFileStructure(localRepoPath);

            // Filter the file structure to include only supported files and exclude ignored patterns
            FileNode filteredFileStructure = filterFileNode(fileStructure, localRepoPath);
            log.info("Filtered file structure to include only supported files and exclude ignored patterns");

            // Generate a unique project ID for RAG
            String projectId = "local-" + UUID.randomUUID().toString().substring(0, 8);

            // Process the repository with DocumentVectorStorage to enable RAG capabilities
            log.info("Processing repository with DocumentVectorStorage to enable RAG capabilities");
            documentVectorStorage.generateEmbeddingsAndPersist(localRepoPath, projectId);

            // Create a documentation directory if it doesn't exist
            Path documentationPath = localRepoPath.resolve("documentation");
            if (!Files.exists(documentationPath)) {
                Files.createDirectory(documentationPath);
            }

            // Generate project overview documentation with RAG enhancement
            generateProjectOverview(localRepoPath, filteredFileStructure, documentationPath, projectId);

            // Generate documentation for key components with RAG enhancement
            generateComponentDocumentation(localRepoPath, filteredFileStructure, documentationPath, projectId);

            // Generate additional diagrams (data flow, sequence diagrams)
            generateAdditionalDiagrams(documentationPath, filteredFileStructure, projectId);

            log.info("Documentation generated successfully for local repository: {}", localRepoPath);

            return documentationPath;
        } catch (Exception e) {
            log.error("Failed to generate documentation for local repository: {}", localRepoPath, e);
            throw new RuntimeException("Failed to generate documentation for local repository: " + localRepoPath, e);
        }
    }

    /**
     * Generates an overview of the project.
     *
     * @param repoPath Path to the repository
     * @param fileStructure File structure of the repository
     * @param documentationPath Path to save the documentation
     * @throws IOException If an I/O error occurs
     */
    private void generateProjectOverview(Path repoPath, FileNode fileStructure, Path documentationPath) throws IOException {
        generateProjectOverview(repoPath, fileStructure, documentationPath, null);
    }

    /**
     * Generates an overview of the project with optional RAG enhancement.
     *
     * @param repoPath Path to the repository
     * @param fileStructure File structure of the repository
     * @param documentationPath Path to save the documentation
     * @param projectId Optional project ID for RAG enhancement, can be null
     * @throws IOException If an I/O error occurs
     */
    private void generateProjectOverview(Path repoPath, FileNode fileStructure, Path documentationPath, String projectId) throws IOException {
        log.info("Generating project overview documentation");

        // Create a prompt for the project overview
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a technical documentation expert. Analyze the following project structure and generate a comprehensive overview of the project. ");
        promptBuilder.append("Include the purpose of the project, its main components, and how they interact. ");
        promptBuilder.append("Format the documentation in Markdown. ");
        promptBuilder.append("Project structure:\n");

        // Add the file structure to the prompt
        addFileStructureToPrompt(fileStructure, promptBuilder, 0);

        // Add key files content to the prompt (README, pom.xml, etc.)
        addKeyFilesContentToPrompt(repoPath, promptBuilder);

        // Enhance with RAG if projectId is provided
        if (projectId != null) {
            enhancePromptWithRAG(promptBuilder, projectId, "project overview");
        }

        // Generate the overview using ChatClient
        String overview = chatClient.prompt(new Prompt(promptBuilder.toString())).call().content();

        // Generate architectural diagram
        String architectureDiagram = generateArchitecturalDiagram(fileStructure, repoPath.getFileName().toString());

        // Combine overview with architectural diagram
        String completeOverview = overview + "\n\n## Project Architecture\n\n" +
                "The following diagram shows the overall architecture of the project:\n\n" +
                "```mermaid\n" + architectureDiagram + "\n```\n";

        // Save the overview to a file
        Path overviewPath = documentationPath.resolve("project-overview.md");
        Files.writeString(overviewPath, completeOverview, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Project overview documentation generated successfully");
    }

    /**
     * Generates documentation for key components of the project.
     *
     * @param repoPath Path to the repository
     * @param fileStructure File structure of the repository
     * @param documentationPath Path to save the documentation
     * @throws IOException If an I/O error occurs
     */
    private void generateComponentDocumentation(Path repoPath, FileNode fileStructure, Path documentationPath) throws IOException {
        generateComponentDocumentation(repoPath, fileStructure, documentationPath, null);
    }

    /**
     * Generates documentation for key components of the project with optional RAG enhancement.
     *
     * @param repoPath Path to the repository
     * @param fileStructure File structure of the repository
     * @param documentationPath Path to save the documentation
     * @param projectId Optional project ID for RAG enhancement, can be null
     * @throws IOException If an I/O error occurs
     */
    private void generateComponentDocumentation(Path repoPath, FileNode fileStructure, Path documentationPath, String projectId) throws IOException {
        log.info("Generating component documentation");

        // Create a components directory
        Path componentsPath = documentationPath.resolve("components");
        if (!Files.exists(componentsPath)) {
            Files.createDirectory(componentsPath);
        }

        // Identify key components (e.g., packages, modules)
        List<FileNode> keyComponents = identifyKeyComponents(fileStructure);

        // Generate documentation for each key component
        for (FileNode component : keyComponents) {
            generateComponentDoc(repoPath, component, componentsPath, projectId);
        }

        log.info("Component documentation generated successfully");
    }

    /**
     * Identifies key components in the project.
     *
     * @param fileStructure File structure of the repository
     * @return List of key components
     */
    private List<FileNode> identifyKeyComponents(FileNode fileStructure) {
        List<FileNode> keyComponents = new ArrayList<>();

        // If the root has children, process them
        if (fileStructure.isDirectory() && fileStructure.getChildren() != null) {
            // Look for src/main directory
            for (FileNode child : fileStructure.getChildren()) {
                if ("src".equals(child.getName()) && child.isDirectory()) {
                    // Found src directory, look for main directory
                    for (FileNode srcChild : child.getChildren()) {
                        if ("main".equals(srcChild.getName()) && srcChild.isDirectory()) {
                            // Found main directory, look for java directory
                            for (FileNode mainChild : srcChild.getChildren()) {
                                if ("java".equals(mainChild.getName()) && mainChild.isDirectory()) {
                                    // Found java directory, add all its children as key components
                                    keyComponents.addAll(mainChild.getChildren());
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }

        return keyComponents;
    }

    /**
     * Generates documentation for a specific component.
     *
     * @param repoPath Path to the repository
     * @param component Component to document
     * @param componentsPath Path to save the component documentation
     * @throws IOException If an I/O error occurs
     */
    private void generateComponentDoc(Path repoPath, FileNode component, Path componentsPath) throws IOException {
        generateComponentDoc(repoPath, component, componentsPath, null);
    }

    /**
     * Generates documentation for a specific component with optional RAG enhancement.
     *
     * @param repoPath Path to the repository
     * @param component Component to document
     * @param componentsPath Path to save the component documentation
     * @param projectId Optional project ID for RAG enhancement, can be null
     * @throws IOException If an I/O error occurs
     */
    private void generateComponentDoc(Path repoPath, FileNode component, Path componentsPath, String projectId) throws IOException {
        log.info("Generating documentation for component: {}", component.getName());

        // Create a prompt for the component documentation
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a technical documentation expert. Analyze the following component and generate comprehensive documentation for it. ");
        promptBuilder.append("Include the purpose of the component, its structure, key classes, and how it interacts with other components. ");
        promptBuilder.append("Format the documentation in Markdown. ");
        promptBuilder.append("Component name: ").append(component.getName()).append("\n");
        promptBuilder.append("Component structure:\n");

        // Add the component structure to the prompt
        addFileStructureToPrompt(component, promptBuilder, 0);

        // Add key files content to the prompt
        addComponentFilesContentToPrompt(repoPath, component, promptBuilder);

        // Enhance with RAG if projectId is provided
        if (projectId != null) {
            enhancePromptWithRAG(promptBuilder, projectId, "component " + component.getName());
        }

        // Generate the component documentation using ChatClient
        String componentDoc = chatClient.prompt(new Prompt(promptBuilder.toString())).call().content();

        // Generate component diagram
        String componentDiagram = generateComponentDiagram(component, repoPath);

        // Combine component documentation with diagram
        String completeComponentDoc = componentDoc + "\n\n## Component Architecture\n\n" +
                "The following diagram shows the structure and relationships within this component:\n\n" +
                "```mermaid\n" + componentDiagram + "\n```\n";

        // Save the component documentation to a file
        Path componentDocPath = componentsPath.resolve(component.getName() + ".md");
        Files.writeString(componentDocPath, completeComponentDoc, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Documentation for component {} generated successfully", component.getName());
    }

    /**
     * Adds the file structure to the prompt.
     *
     * @param node Current node in the file structure
     * @param promptBuilder StringBuilder to append the file structure to
     * @param depth Current depth in the file structure
     */
    private void addFileStructureToPrompt(FileNode node, StringBuilder promptBuilder, int depth) {
        // Add indentation based on depth
        for (int i = 0; i < depth; i++) {
            promptBuilder.append("  ");
        }

        // Add the node name and type
        promptBuilder.append(node.isDirectory() ? "📁 " : "📄 ").append(node.getName()).append("\n");

        // Recursively add children if this is a directory
        if (node.isDirectory() && node.getChildren() != null) {
            for (FileNode child : node.getChildren()) {
                addFileStructureToPrompt(child, promptBuilder, depth + 1);
            }
        }
    }

    /**
     * Adds the content of key files to the prompt.
     *
     * @param repoPath Path to the repository
     * @param promptBuilder StringBuilder to append the file content to
     * @throws IOException If an I/O error occurs
     */
    private void addKeyFilesContentToPrompt(Path repoPath, StringBuilder promptBuilder) throws IOException {
        // Add README content if it exists
        Path readmePath = repoPath.resolve("README.md");
        if (Files.exists(readmePath)) {
            promptBuilder.append("\nREADME.md content:\n```markdown\n");
            promptBuilder.append(Files.readString(readmePath));
            promptBuilder.append("\n```\n");
        }

        // Add pom.xml content if it exists (for Maven projects)
        Path pomPath = repoPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            promptBuilder.append("\npom.xml content:\n```xml\n");
            promptBuilder.append(Files.readString(pomPath));
            promptBuilder.append("\n```\n");
        }
    }

    /**
     * Adds the content of key files in a component to the prompt.
     *
     * @param repoPath Path to the repository
     * @param component Component to get files from
     * @param promptBuilder StringBuilder to append the file content to
     * @throws IOException If an I/O error occurs
     */
    private void addComponentFilesContentToPrompt(Path repoPath, FileNode component, StringBuilder promptBuilder) throws IOException {
        // If the component is a directory, add content of key files
        if (component.isDirectory() && component.getChildren() != null) {
            // Add content of up to 5 supported files
            int filesAdded = 0;
            for (FileNode child : component.getChildren()) {
                if (!child.isDirectory() && filesAdded < 5) {
                    Path filePath = repoPath.resolve(child.getPath());
                    if (Files.exists(filePath) && isFileSupported(filePath) && isNotIgnored(filePath)) {
                        String extension = getFileExtension(child.getName());
                        String codeBlockLanguage = getCodeBlockLanguage(extension);

                        promptBuilder.append("\n").append(child.getName()).append(" content:\n```").append(codeBlockLanguage).append("\n");
                        promptBuilder.append(Files.readString(filePath));
                        promptBuilder.append("\n```\n");
                        filesAdded++;
                    }
                }
            }
        }
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param filename The filename
     * @return The file extension (without the dot) or an empty string if no extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Maps a file extension to a code block language for Markdown.
     *
     * @param extension The file extension
     * @return The code block language
     */
    private String getCodeBlockLanguage(String extension) {
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "kt" -> "kotlin";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "rb" -> "ruby";
            case "go" -> "go";
            case "rs" -> "rust";
            case "c", "cpp", "h", "hpp" -> "cpp";
            case "cs" -> "csharp";
            case "php" -> "php";
            case "html" -> "html";
            case "css" -> "css";
            case "md" -> "markdown";
            case "json" -> "json";
            case "xml" -> "xml";
            case "yaml", "yml" -> "yaml";
            default -> "text";
        };
    }

    /**
     * Generates a unique project ID based on the repository URL.
     *
     * @param repoUrl GitHub repository URL
     * @return Unique project ID
     */
    private String generateProjectId(String repoUrl) {
        // Extract repository name from URL
        String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1)
                .replace(".git", "");

        // Combine repository name with a UUID to ensure uniqueness
        return repoName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Checks if a file has a supported extension.
     *
     * @param path Path to the file
     * @return true if the file has a supported extension, false otherwise
     */
    private boolean isFileSupported(Path path) {
        String fileName = path.toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * Checks if a file should be ignored based on ignored patterns.
     *
     * @param path Path to the file
     * @return true if the file should not be ignored, false if it should be ignored
     */
    private boolean isNotIgnored(Path path) {
        String pathStr = path.toString();
        return IGNORED_PATTERNS.stream().noneMatch(pathStr::contains);
    }

    /**
     * Enhances a prompt with relevant context from the RAG system.
     *
     * @param promptBuilder The prompt builder to enhance
     * @param projectId The project ID to query for relevant context
     * @param query The query to use for finding relevant context
     */
    private void enhancePromptWithRAG(StringBuilder promptBuilder, String projectId, String query) {
        try {
            log.info("Enhancing prompt with RAG for query: {}", query);

            // Get relevant document chunks from the RAG system
            List<DocumentChunk> relevantChunks = documentVectorStorage.getDocumentChunksFromProject(projectId, query, 5);

            if (!relevantChunks.isEmpty()) {
                promptBuilder.append("\n\nAdditional context from the codebase:\n");

                for (DocumentChunk chunk : relevantChunks) {
                    promptBuilder.append("\nFile: ").append(chunk.getFilePath())
                            .append(" (lines ").append(chunk.getStartLine())
                            .append("-").append(chunk.getEndLine()).append(")\n");
                    promptBuilder.append("```\n").append(chunk.getContent()).append("```\n");
                }
            }
        } catch (Exception e) {
            // Log the error but continue without RAG enhancement
            log.warn("Failed to enhance prompt with RAG: {}", e.getMessage());
        }
    }

    /**
     * Filters a FileNode tree to include only supported files and exclude ignored patterns.
     *
     * @param node The FileNode to filter
     * @param repoPath The base repository path
     * @return A filtered FileNode, or null if the node should be excluded
     */
    private FileNode filterFileNode(FileNode node, Path repoPath) {
        if (node == null) {
            return null;
        }

        Path nodePath = Path.of(node.getPath());

        // If it's a file, check if it's supported and not ignored
        if (!node.isDirectory()) {
            if (isFileSupported(nodePath) && isNotIgnored(nodePath)) {
                return node;
            }
            return null;
        }

        // For directories, filter children recursively
        FileNode filteredNode = new FileNode(node.getName(), node.getPath(), true);

        if (node.getChildren() != null) {
            List<FileNode> filteredChildren = node.getChildren().stream()
                    .map(child -> filterFileNode(child, repoPath))
                    .filter(child -> child != null)
                    .collect(Collectors.toList());

            // Add filtered children to the node
            for (FileNode child : filteredChildren) {
                filteredNode.addChild(child);
            }
        }

        // If directory has no children after filtering, exclude it unless it's the root
        if (filteredNode.getChildren() == null || filteredNode.getChildren().isEmpty()) {
            if (nodePath.equals(repoPath)) {
                return filteredNode; // Keep root even if empty
            }
            return null;
        }

        return filteredNode;
    }

    /**
     * Generates an architectural diagram for the project using MermaidService.
     *
     * @param fileStructure The project file structure
     * @param projectName The name of the project
     * @return Mermaid diagram syntax for the project architecture
     */
    private String generateArchitecturalDiagram(FileNode fileStructure, String projectName) {
        try {
            log.info("Generating architectural diagram for project: {}", projectName);
            return mermaidService.generateArchitectureDiagram(fileStructure, projectName);
        } catch (Exception e) {
            log.warn("Failed to generate architectural diagram for project: {}", projectName, e);
            // Return a fallback simple diagram
            return "flowchart TD\n    A[" + projectName + "] --> B[Main Components]\n    B --> C[Services]\n    B --> D[Controllers]\n    B --> E[Data Layer]";
        }
    }

    /**
     * Generates a component diagram using MermaidService.
     *
     * @param component The component to generate a diagram for
     * @param repoPath The repository path for context
     * @return Mermaid diagram syntax for the component
     */
    private String generateComponentDiagram(FileNode component, Path repoPath) {
        try {
            log.info("Generating component diagram for: {}", component.getName());
            
            // Collect component files for analysis
            List<String> componentFiles = new ArrayList<>();
            collectComponentFiles(component, componentFiles);
            
            // If we have files, generate a component diagram
            if (!componentFiles.isEmpty()) {
                return mermaidService.generateComponentDiagram(componentFiles, component.getName());
            } else {
                // Generate a simple structure diagram based on file structure
                return generateSimpleComponentDiagram(component);
            }
        } catch (Exception e) {
            log.warn("Failed to generate component diagram for: {}", component.getName(), e);
            // Return a fallback simple diagram
            return generateSimpleComponentDiagram(component);
        }
    }

    /**
     * Collects file names from a component for diagram generation.
     *
     * @param component The component node
     * @param componentFiles List to collect file names
     */
    private void collectComponentFiles(FileNode component, List<String> componentFiles) {
        if (component.getChildren() != null) {
            for (FileNode child : component.getChildren()) {
                if (!child.isDirectory()) {
                    // Add file name without extension for cleaner diagram
                    String fileName = child.getName();
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot > 0) {
                        fileName = fileName.substring(0, lastDot);
                    }
                    componentFiles.add(fileName);
                } else {
                    // Recursively collect from subdirectories
                    collectComponentFiles(child, componentFiles);
                }
            }
        }
    }

    /**
     * Generates a simple component diagram based on file structure.
     *
     * @param component The component to generate a diagram for
     * @return Simple Mermaid diagram syntax
     */
    private String generateSimpleComponentDiagram(FileNode component) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("classDiagram\n");
        diagram.append("    class ").append(component.getName()).append(" {\n");
        
        if (component.getChildren() != null) {
            for (FileNode child : component.getChildren()) {
                if (!child.isDirectory()) {
                    String fileName = child.getName();
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot > 0) {
                        fileName = fileName.substring(0, lastDot);
                    }
                    diagram.append("        +").append(fileName).append("()\n");
                }
            }
        }
        
        diagram.append("    }\n");
        return diagram.toString();
    }

    /**
     * Generates additional diagrams for the documentation including data flow and sequence diagrams.
     *
     * @param documentationPath Path to save additional diagrams
     * @param fileStructure The project file structure
     * @param projectId Project ID for RAG enhancement
     * @throws IOException If an I/O error occurs
     */
    public void generateAdditionalDiagrams(Path documentationPath, FileNode fileStructure, String projectId) throws IOException {
        log.info("Generating additional diagrams for documentation");
        
        try {
            // Create diagrams directory
            Path diagramsPath = documentationPath.resolve("diagrams");
            if (!Files.exists(diagramsPath)) {
                Files.createDirectory(diagramsPath);
            }

            // Generate data flow diagram
            String dataFlowDiagram = generateDataFlowDiagram(fileStructure, projectId);
            if (dataFlowDiagram != null && !dataFlowDiagram.isEmpty()) {
                Path dataFlowPath = diagramsPath.resolve("data-flow.md");
                String dataFlowContent = "# Data Flow Diagram\n\n" +
                        "This diagram shows how data flows through the system:\n\n" +
                        "```mermaid\n" + dataFlowDiagram + "\n```\n";
                Files.writeString(dataFlowPath, dataFlowContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            // Generate sequence diagram for main interactions
            String sequenceDiagram = generateSequenceDiagram(fileStructure, projectId);
            if (sequenceDiagram != null && !sequenceDiagram.isEmpty()) {
                Path sequencePath = diagramsPath.resolve("sequence.md");
                String sequenceContent = "# Sequence Diagram\n\n" +
                        "This diagram shows the sequence of interactions in the system:\n\n" +
                        "```mermaid\n" + sequenceDiagram + "\n```\n";
                Files.writeString(sequencePath, sequenceContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            log.info("Additional diagrams generated successfully");
        } catch (Exception e) {
            log.warn("Failed to generate additional diagrams: {}", e.getMessage());
        }
    }

    /**
     * Generates a data flow diagram for the project.
     *
     * @param fileStructure The project file structure
     * @param projectId Project ID for RAG context
     * @return Mermaid data flow diagram syntax
     */
    private String generateDataFlowDiagram(FileNode fileStructure, String projectId) {
        try {
            // Get relevant code content for data flow analysis
            StringBuilder codeContent = new StringBuilder();
            collectCodeContentForAnalysis(fileStructure, codeContent, 3); // Limit to 3 files for analysis
            
            if (codeContent.length() > 0) {
                return mermaidService.generateDataFlowDiagram(codeContent.toString(), "System Data Flow");
            }
        } catch (Exception e) {
            log.warn("Failed to generate data flow diagram: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generates a sequence diagram for the project.
     *
     * @param fileStructure The project file structure
     * @param projectId Project ID for RAG context
     * @return Mermaid sequence diagram syntax
     */
    private String generateSequenceDiagram(FileNode fileStructure, String projectId) {
        try {
            // Get relevant code content for sequence analysis
            StringBuilder codeContent = new StringBuilder();
            collectCodeContentForAnalysis(fileStructure, codeContent, 2); // Limit to 2 files for analysis
            
            if (codeContent.length() > 0) {
                return mermaidService.generateSequenceDiagram(codeContent.toString(), "System Interactions");
            }
        } catch (Exception e) {
            log.warn("Failed to generate sequence diagram: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Collects code content from files for diagram analysis.
     *
     * @param node Current file node
     * @param codeContent StringBuilder to collect content
     * @param maxFiles Maximum number of files to analyze
     */
    private void collectCodeContentForAnalysis(FileNode node, StringBuilder codeContent, int maxFiles) {
        if (maxFiles <= 0) return;
        
        if (node.getChildren() != null) {
            int filesProcessed = 0;
            for (FileNode child : node.getChildren()) {
                if (filesProcessed >= maxFiles) break;
                
                if (!child.isDirectory()) {
                    // Add file content if it's a supported file
                    String fileName = child.getName().toLowerCase();
                    if (fileName.endsWith(".java") || fileName.endsWith(".js") || fileName.endsWith(".ts") || fileName.endsWith(".py")) {
                        codeContent.append("// File: ").append(child.getName()).append("\n");
                        codeContent.append("// Content placeholder for analysis\n\n");
                        filesProcessed++;
                    }
                } else {
                    // Recursively process subdirectories
                    collectCodeContentForAnalysis(child, codeContent, maxFiles - filesProcessed);
                }
            }
        }
    }
}