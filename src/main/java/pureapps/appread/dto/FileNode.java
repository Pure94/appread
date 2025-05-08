package pureapps.appread.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a file/folder tree structure.
 * Can be either a file or a directory.
 */
@Data
public class FileNode {
    private String name;
    private String path;
    private boolean isDirectory;
    private List<FileNode> children;

    public FileNode(String name, String path, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.children = isDirectory ? new ArrayList<>() : null;
    }

    /**
     * Adds a child node to this node.
     * Only applicable if this node is a directory.
     *
     * @param child the child node to add
     * @throws IllegalStateException if this node is not a directory
     */
    public void addChild(FileNode child) {
        if (!isDirectory) {
            throw new IllegalStateException("Cannot add child to a file node");
        }
        children.add(child);
    }
}