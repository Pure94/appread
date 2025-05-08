<template>
  <div class="diagram">
    <h1>Code Visualization</h1>
    
    <div class="diagram-container">
      <div class="diagram-options">
        <h2>Generate Diagram</h2>
        <form @submit.prevent="generateDiagram">
          <div class="form-group">
            <label for="diagramType">Diagram Type</label>
            <select id="diagramType" v-model="diagramType">
              <option value="class">Class Diagram</option>
              <option value="sequence">Sequence Diagram</option>
              <option value="component">Component Diagram</option>
              <option value="dependency">Dependency Graph</option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="scope">Scope</label>
            <select id="scope" v-model="scope">
              <option value="all">Entire Repository</option>
              <option value="package">Specific Package/Directory</option>
              <option value="class">Specific Class</option>
            </select>
          </div>
          
          <div class="form-group" v-if="scope !== 'all'">
            <label for="scopePath">Path or Class Name</label>
            <input 
              type="text" 
              id="scopePath" 
              v-model="scopePath" 
              placeholder="Enter path or class name"
            >
          </div>
          
          <button type="submit" class="btn" :disabled="isLoading">
            {{ isLoading ? 'Generating...' : 'Generate Diagram' }}
          </button>
        </form>
      </div>
      
      <div class="diagram-display">
        <div v-if="!diagramUrl" class="empty-state">
          <p>No diagram generated yet. Configure options and click "Generate Diagram".</p>
        </div>
        <div v-else class="diagram-content">
          <h3>{{ diagramTitle }}</h3>
          <div class="diagram-image">
            <!-- This would be replaced with an actual diagram rendering component -->
            <div class="placeholder-diagram">
              <p>Diagram visualization would appear here</p>
              <p>Type: {{ diagramType }}</p>
              <p>Scope: {{ scope === 'all' ? 'Entire Repository' : scopePath }}</p>
            </div>
          </div>
          <div class="diagram-actions">
            <button class="btn btn-secondary">Download SVG</button>
            <button class="btn btn-secondary">Download PNG</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue'

export default defineComponent({
  name: 'DiagramView',
  
  setup() {
    const diagramType = ref('class');
    const scope = ref('all');
    const scopePath = ref('');
    const isLoading = ref(false);
    const diagramUrl = ref('');
    
    const diagramTitle = computed(() => {
      const typeLabel = {
        'class': 'Class Diagram',
        'sequence': 'Sequence Diagram',
        'component': 'Component Diagram',
        'dependency': 'Dependency Graph'
      }[diagramType.value] || 'Diagram';
      
      if (scope.value === 'all') {
        return `${typeLabel} - Full Repository`;
      } else {
        return `${typeLabel} - ${scopePath.value}`;
      }
    });
    
    const generateDiagram = async () => {
      if (scope.value !== 'all' && !scopePath.value.trim()) {
        alert('Please enter a path or class name for the selected scope.');
        return;
      }
      
      isLoading.value = true;
      
      try {
        // Simulate diagram generation (would be replaced with actual API call)
        setTimeout(() => {
          // In a real implementation, this would be a URL to the generated diagram
          diagramUrl.value = 'placeholder-diagram-url';
          isLoading.value = false;
        }, 1500);
      } catch (error) {
        console.error('Error generating diagram:', error);
        alert('There was an error generating the diagram. Please try again.');
        isLoading.value = false;
      }
    };
    
    return {
      diagramType,
      scope,
      scopePath,
      isLoading,
      diagramUrl,
      diagramTitle,
      generateDiagram
    };
  }
});
</script>

<style scoped>
.diagram {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  font-size: 2rem;
  margin-bottom: 2rem;
  color: #2c3e50;
}

.diagram-container {
  display: flex;
  gap: 2rem;
}

.diagram-options {
  flex: 1;
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  height: fit-content;
}

.diagram-display {
  flex: 2;
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
  color: #2c3e50;
}

input, select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #95a5a6;
  font-style: italic;
  text-align: center;
}

.diagram-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.diagram-image {
  flex: 1;
  background-color: #f8f9fa;
  border-radius: 4px;
  margin: 1rem 0;
  overflow: auto;
}

.placeholder-diagram {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 300px;
  color: #95a5a6;
  border: 2px dashed #ddd;
  border-radius: 4px;
  padding: 2rem;
}

.diagram-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn {
  display: inline-block;
  background-color: #2c3e50;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.3s ease;
}

.btn:hover:not(:disabled) {
  background-color: #1e2b3a;
}

.btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #95a5a6;
}

.btn-secondary:hover {
  background-color: #7f8c8d;
}

@media (max-width: 768px) {
  .diagram-container {
    flex-direction: column;
  }
}
</style>