<template>
  <div class="file-tree-component">
    <ul class="file-tree-list">
      <li v-for="node in nodes" :key="node.path" class="file-tree-item">
        <div class="file-item" @click="toggleNode(node)">
          <span class="icon">
            {{ node.isDirectory ? (expandedNodes.includes(node.path) ? '📂' : '📁') : '📄' }}
          </span>
          <span class="name">{{ node.name }}</span>
        </div>
        
        <!-- Recursively render children if this is a directory and it's expanded -->
        <div v-if="node.isDirectory && expandedNodes.includes(node.path)" class="children">
          <FileTree 
            :nodes="node.children" 
            :expanded-nodes="expandedNodes"
            @toggle="onChildToggle"
            @select="onChildSelect"
          />
        </div>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from 'vue';

interface FileNode {
  name: string;
  path: string;
  isDirectory: boolean;
  children: FileNode[] | null;
}

export default defineComponent({
  name: 'FileTree',
  
  props: {
    nodes: {
      type: Array as PropType<FileNode[]>,
      required: true
    },
    expandedNodes: {
      type: Array as PropType<string[]>,
      default: () => []
    }
  },
  
  emits: ['toggle', 'select'],
  
  setup(props, { emit }) {
    const toggleNode = (node: FileNode) => {
      if (node.isDirectory) {
        emit('toggle', node.path);
      } else {
        emit('select', node);
      }
    };
    
    const onChildToggle = (path: string) => {
      emit('toggle', path);
    };
    
    const onChildSelect = (node: FileNode) => {
      emit('select', node);
    };
    
    return {
      toggleNode,
      onChildToggle,
      onChildSelect
    };
  }
});
</script>

<style scoped>
.file-tree-list {
  list-style-type: none;
  padding-left: 0;
  margin: 0;
}

.file-tree-item {
  margin: 4px 0;
}

.file-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.file-item:hover {
  background-color: #f5f5f5;
}

.icon {
  margin-right: 8px;
}

.children {
  padding-left: 20px;
}
</style>