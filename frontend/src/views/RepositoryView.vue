<template>
  <div class="repository">
    <h1>Repository Analysis</h1>

    <!-- Repository Input Form -->
    <div class="repository-form" v-if="!repository.path">
      <h2>Clone a Repository</h2>
      <form @submit.prevent="cloneRepository">
        <div class="form-group">
          <label for="repoUrl">Repository URL</label>
          <input 
            type="text" 
            id="repoUrl" 
            v-model="repoUrl" 
            placeholder="https://github.com/username/repository.git"
            required
          >
        </div>

        <div class="form-group">
          <label for="token">Personal Access Token (optional)</label>
          <input 
            type="password" 
            id="token" 
            v-model="token" 
            placeholder="For private repositories"
          >
        </div>

        <button type="submit" class="btn" :disabled="isLoading">
          {{ isLoading ? 'Cloning...' : 'Clone Repository' }}
        </button>
      </form>

      <div class="error" v-if="repository.error">
        {{ repository.error }}
      </div>
    </div>

    <!-- Repository Explorer -->
    <div class="repository-explorer" v-else>
      <div class="repository-info">
        <h2>{{ repository.name }}</h2>
        <p>{{ repository.url }}</p>
        <button class="btn btn-secondary" @click="resetRepository">Clone Another Repository</button>
      </div>

      <div class="repository-content">
        <div class="file-tree">
          <h3>Files</h3>
          <div v-if="fileStructure && fileStructure.children && fileStructure.children.length">
            <FileTree 
              :nodes="fileStructure.children" 
              :expanded-nodes="expandedNodes"
              @toggle="toggleNode"
              @select="selectFile"
            />
          </div>
          <p v-else>No files found in the repository.</p>
        </div>

        <div class="file-content">
          <h3>File Content</h3>
          <div v-if="selectedFile">
            <h4>{{ selectedFile.path }}</h4>
            <pre class="file-content-text">{{ selectedFile.content }}</pre>
          </div>
          <p v-else>Select a file to view its content.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import { useRepositoryStore } from '@/stores/repository'
import { storeToRefs } from 'pinia'
import FileTree from '@/components/FileTree.vue'

export default defineComponent({
  name: 'RepositoryView',

  components: {
    FileTree
  },

  setup() {
    const repositoryStore = useRepositoryStore()
    const { repository, fileStructure } = storeToRefs(repositoryStore)

    const repoUrl = ref('')
    const token = ref('')
    const expandedNodes = ref<string[]>([])
    const selectedFile = ref<{ path: string, content: string } | null>(null)

    const cloneRepository = async () => {
      await repositoryStore.cloneRepository(repoUrl.value, token.value || undefined)
    }

    const resetRepository = () => {
      repository.value = {
        url: '',
        name: '',
        path: '',
        isLoading: false,
        error: null
      }
      repoUrl.value = ''
      token.value = ''
      expandedNodes.value = []
      selectedFile.value = null
    }

    const toggleNode = (path: string) => {
      const index = expandedNodes.value.indexOf(path)
      if (index === -1) {
        expandedNodes.value.push(path)
      } else {
        expandedNodes.value.splice(index, 1)
      }
    }

    const selectFile = async (node: any) => {
      try {
        // Here you would typically fetch the file content from the backend
        // For now, we'll just set the path
        selectedFile.value = {
          path: node.path,
          content: `Content of ${node.name} would be loaded here`
        }
      } catch (error) {
        console.error('Error loading file content:', error)
      }
    }

    return {
      repository,
      fileStructure,
      repoUrl,
      token,
      expandedNodes,
      selectedFile,
      isLoading: repository.value.isLoading,
      cloneRepository,
      resetRepository,
      toggleNode,
      selectFile
    }
  }
})
</script>

<style scoped>
.repository {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  font-size: 2rem;
  margin-bottom: 2rem;
  color: #2c3e50;
}

.repository-form {
  background-color: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  margin: 0 auto;
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

input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
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

.error {
  color: #e74c3c;
  margin-top: 1rem;
}

.repository-explorer {
  margin-top: 2rem;
}

.repository-info {
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.repository-content {
  display: flex;
  gap: 2rem;
}

.file-tree, .file-content {
  flex: 1;
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

li:hover {
  background-color: #f8f9fa;
}

.file-content-text {
  background-color: #f8f9fa;
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  max-height: 500px;
  overflow-y: auto;
}
</style>
