package pureapps.appread.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.DocumentVectorStorage;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.dto.FileNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

            // Create a documentation directory if it doesn't exist
            Path documentationPath = localRepoPath.resolve("documentation");
            if (!Files.exists(documentationPath)) {
                Files.createDirectory(documentationPath);
            }

            // Generate project overview documentation
            generateProjectOverview(localRepoPath, fileStructure, documentationPath);

            // Generate documentation for key components
            generateComponentDocumentation(localRepoPath, fileStructure, documentationPath);

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

        // Generate the overview using ChatClient
        String overview = chatClient.prompt(new Prompt(promptBuilder.toString())).call().content();

        // Save the overview to a file
        Path overviewPath = documentationPath.resolve("project-overview.md");
        Files.writeString(overviewPath, overview, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

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
            generateComponentDoc(repoPath, component, componentsPath);
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

        // Generate the component documentation using ChatClient
        String componentDoc = chatClient.prompt(new Prompt(promptBuilder.toString())).call().content();

        // Save the component documentation to a file
        Path componentDocPath = componentsPath.resolve(component.getName() + ".md");
        Files.writeString(componentDocPath, componentDoc, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

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
            // Add content of up to 5 Java files
            int filesAdded = 0;
            for (FileNode child : component.getChildren()) {
                if (!child.isDirectory() && child.getName().endsWith(".java") && filesAdded < 5) {
                    Path filePath = repoPath.resolve(child.getPath());
                    if (Files.exists(filePath)) {
                        promptBuilder.append("\n").append(child.getName()).append(" content:\n```java\n");
                        promptBuilder.append(Files.readString(filePath));
                        promptBuilder.append("\n```\n");
                        filesAdded++;
                    }
                }
            }
        }
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
}
