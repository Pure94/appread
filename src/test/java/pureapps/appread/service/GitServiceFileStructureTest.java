package pureapps.appread.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pureapps.appread.dto.FileNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceFileStructureTest {

    private GitService gitService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        gitService = new GitService(tempDir.toString(), 24);
    }
    
    @Test
    void testGetFileStructure() throws IOException {
        // Create a test directory structure
        Path repoPath = tempDir.resolve("test-repo");
        Files.createDirectories(repoPath);
        
        // Create some files and directories
        Path srcDir = repoPath.resolve("src");
        Path mainDir = srcDir.resolve("main");
        Path testDir = srcDir.resolve("test");
        Path javaDir = mainDir.resolve("java");
        Path resourcesDir = mainDir.resolve("resources");
        
        Files.createDirectories(javaDir);
        Files.createDirectories(resourcesDir);
        Files.createDirectories(testDir);
        
        // Create some files
        Files.writeString(repoPath.resolve("README.md"), "# Test Repository");
        Files.writeString(javaDir.resolve("Main.java"), "public class Main { }");
        Files.writeString(resourcesDir.resolve("application.properties"), "app.name=Test");
        Files.writeString(testDir.resolve("Test.java"), "public class Test { }");
        
        // Create a hidden directory that should be skipped
        Path gitDir = repoPath.resolve(".git");
        Files.createDirectories(gitDir);
        Files.writeString(gitDir.resolve("config"), "# Git config");
        
        // Get the file structure
        FileNode rootNode = gitService.getFileStructure(repoPath);
        
        // Verify the root node
        assertEquals("test-repo", rootNode.getName());
        assertEquals(repoPath.toString(), rootNode.getPath());
        assertTrue(rootNode.isDirectory());
        
        // Verify the children
        List<FileNode> rootChildren = rootNode.getChildren();
        assertEquals(2, rootChildren.size()); // README.md and src (skipping .git)
        
        // Find the src directory
        FileNode srcNode = findNodeByName(rootChildren, "src");
        assertNotNull(srcNode);
        assertTrue(srcNode.isDirectory());
        
        // Verify src children
        List<FileNode> srcChildren = srcNode.getChildren();
        assertEquals(2, srcChildren.size()); // main and test
        
        // Find the main directory
        FileNode mainNode = findNodeByName(srcChildren, "main");
        assertNotNull(mainNode);
        assertTrue(mainNode.isDirectory());
        
        // Verify main children
        List<FileNode> mainChildren = mainNode.getChildren();
        assertEquals(2, mainChildren.size()); // java and resources
        
        // Find the java directory
        FileNode javaNode = findNodeByName(mainChildren, "java");
        assertNotNull(javaNode);
        assertTrue(javaNode.isDirectory());
        
        // Verify java children
        List<FileNode> javaChildren = javaNode.getChildren();
        assertEquals(1, javaChildren.size()); // Main.java
        
        // Verify Main.java
        FileNode mainJavaNode = javaChildren.get(0);
        assertEquals("Main.java", mainJavaNode.getName());
        assertFalse(mainJavaNode.isDirectory());
        assertNull(mainJavaNode.getChildren());
    }
    
    @Test
    void testGetFileStructureWithInvalidPath() {
        // Test with null path
        assertThrows(IllegalArgumentException.class, () -> gitService.getFileStructure(null));
        
        // Test with non-existent path
        Path nonExistentPath = tempDir.resolve("non-existent");
        assertThrows(IllegalArgumentException.class, () -> gitService.getFileStructure(nonExistentPath));
    }
    
    private FileNode findNodeByName(List<FileNode> nodes, String name) {
        return nodes.stream()
                .filter(node -> node.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}