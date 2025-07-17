package pureapps.appread.mermaid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import pureapps.appread.dto.FileNode;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MermaidService {

    private final ChatClient chatClient;

    /**
     * Generates a Mermaid diagram based on a description.
     *
     * @param description Description of what the diagram should represent
     * @return Mermaid diagram syntax
     */
    public String generateMermaidSyntax(String description) {
        String prompt = "Generate only Mermaid diagram code based on the following description, without any additional comments or explanations. " +
                "Return only the Mermaid syntax that can be directly used:\n" + description;
        
        try {
            String result = chatClient.prompt(new Prompt(prompt)).call().content();
            log.debug("Generated Mermaid diagram for description: {}", description);
            return cleanMermaidSyntax(result);
        } catch (Exception e) {
            log.error("Failed to generate Mermaid diagram for description: {}", description, e);
            throw new RuntimeException("Failed to generate Mermaid diagram", e);
        }
    }

    /**
     * Generates a project architecture diagram based on file structure.
     *
     * @param fileStructure The project file structure
     * @param projectName Name of the project
     * @return Mermaid flowchart diagram showing project architecture
     */
    public String generateArchitectureDiagram(FileNode fileStructure, String projectName) {
        log.info("Generating architecture diagram for project: {}", projectName);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a Mermaid flowchart diagram showing the architecture of the project '")
              .append(projectName)
              .append("'. Based on the following file structure, create a diagram that shows the main components and their relationships. ")
              .append("Focus on packages, modules, and key files. Use flowchart syntax. ")
              .append("Return only the Mermaid syntax:\n\n");
        
        addFileStructureToPrompt(fileStructure, prompt, 0);
        
        try {
            String result = chatClient.prompt(new Prompt(prompt.toString())).call().content();
            log.debug("Generated architecture diagram for project: {}", projectName);
            return cleanMermaidSyntax(result);
        } catch (Exception e) {
            log.error("Failed to generate architecture diagram for project: {}", projectName, e);
            throw new RuntimeException("Failed to generate architecture diagram", e);
        }
    }

    /**
     * Generates a component relationship diagram.
     *
     * @param components List of key components
     * @param componentName Name of the main component
     * @return Mermaid class diagram showing component relationships
     */
    public String generateComponentDiagram(List<String> components, String componentName) {
        log.info("Generating component diagram for: {}", componentName);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a Mermaid class diagram showing the relationships between the following components in '")
              .append(componentName)
              .append("'. Show dependencies, inheritance, and associations. ")
              .append("Return only the Mermaid syntax:\n\n");
        
        for (String component : components) {
            prompt.append("- ").append(component).append("\n");
        }
        
        try {
            String result = chatClient.prompt(new Prompt(prompt.toString())).call().content();
            log.debug("Generated component diagram for: {}", componentName);
            return cleanMermaidSyntax(result);
        } catch (Exception e) {
            log.error("Failed to generate component diagram for: {}", componentName, e);
            throw new RuntimeException("Failed to generate component diagram", e);
        }
    }

    /**
     * Generates a data flow diagram based on file content analysis.
     *
     * @param fileContent Content of files to analyze
     * @param title Title for the diagram
     * @return Mermaid flowchart showing data flow
     */
    public String generateDataFlowDiagram(String fileContent, String title) {
        log.info("Generating data flow diagram: {}", title);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following code and generate a Mermaid flowchart diagram showing the data flow. ")
              .append("Show how data moves through the system, including inputs, processing steps, and outputs. ")
              .append("Title: ").append(title).append("\n")
              .append("Return only the Mermaid syntax:\n\n")
              .append("Code to analyze:\n")
              .append(fileContent);
        
        try {
            String result = chatClient.prompt(new Prompt(prompt.toString())).call().content();
            log.debug("Generated data flow diagram: {}", title);
            return cleanMermaidSyntax(result);
        } catch (Exception e) {
            log.error("Failed to generate data flow diagram: {}", title, e);
            throw new RuntimeException("Failed to generate data flow diagram", e);
        }
    }

    /**
     * Generates a sequence diagram based on method interactions.
     *
     * @param methodContent Content showing method interactions
     * @param title Title for the sequence diagram
     * @return Mermaid sequence diagram
     */
    public String generateSequenceDiagram(String methodContent, String title) {
        log.info("Generating sequence diagram: {}", title);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following code and generate a Mermaid sequence diagram showing the interaction between different components/methods. ")
              .append("Show the sequence of calls and responses. ")
              .append("Title: ").append(title).append("\n")
              .append("Return only the Mermaid syntax:\n\n")
              .append("Code to analyze:\n")
              .append(methodContent);
        
        try {
            String result = chatClient.prompt(new Prompt(prompt.toString())).call().content();
            log.debug("Generated sequence diagram: {}", title);
            return cleanMermaidSyntax(result);
        } catch (Exception e) {
            log.error("Failed to generate sequence diagram: {}", title, e);
            throw new RuntimeException("Failed to generate sequence diagram", e);
        }
    }

    /**
     * Cleans the Mermaid syntax by removing markdown code blocks and extra text.
     *
     * @param rawOutput Raw output from the LLM
     * @return Cleaned Mermaid syntax
     */
    private String cleanMermaidSyntax(String rawOutput) {
        if (rawOutput == null) {
            return "";
        }
        
        // Remove markdown code blocks
        String cleaned = rawOutput.replaceAll("```mermaid\\s*", "")
                                 .replaceAll("```\\s*", "")
                                 .trim();
        
        // If the output doesn't start with a Mermaid diagram type, try to extract it
        if (!cleaned.matches("^(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gitgraph).*")) {
            // Look for Mermaid syntax within the text
            String[] lines = cleaned.split("\n");
            StringBuilder mermaidContent = new StringBuilder();
            boolean foundMermaid = false;
            
            for (String line : lines) {
                if (line.trim().matches("^(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gitgraph).*")) {
                    foundMermaid = true;
                }
                if (foundMermaid) {
                    mermaidContent.append(line).append("\n");
                }
            }
            
            if (foundMermaid) {
                cleaned = mermaidContent.toString().trim();
            }
        }
        
        return cleaned;
    }

    /**
     * Adds file structure to prompt for diagram generation.
     *
     * @param node Current file node
     * @param prompt StringBuilder to append to
     * @param depth Current depth level
     */
    private void addFileStructureToPrompt(FileNode node, StringBuilder prompt, int depth) {
        if (depth > 3) { // Limit depth to avoid overly complex diagrams
            return;
        }
        
        // Add indentation
        for (int i = 0; i < depth; i++) {
            prompt.append("  ");
        }
        
        prompt.append(node.isDirectory() ? "📁 " : "📄 ").append(node.getName()).append("\n");
        
        // Add children if directory
        if (node.isDirectory() && node.getChildren() != null) {
            for (FileNode child : node.getChildren()) {
                addFileStructureToPrompt(child, prompt, depth + 1);
            }
        }
    }
}