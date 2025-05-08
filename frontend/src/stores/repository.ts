import { defineStore } from 'pinia'
import axios from 'axios'

interface Repository {
  url: string
  name: string
  path: string
  isLoading: boolean
  error: string | null
}

interface FileNode {
  name: string
  path: string
  isDirectory: boolean
  children: FileNode[] | null
}

export const useRepositoryStore = defineStore('repository', {
  state: () => ({
    repository: {
      url: '',
      name: '',
      path: '',
      isLoading: false,
      error: null
    } as Repository,
    fileStructure: null as FileNode | null
  }),

  actions: {
    async cloneRepository(url: string, token?: string) {
      this.repository.isLoading = true
      this.repository.error = null

      try {
        const response = await axios.post('http://localhost:8080/api/repository/clone', {
          url,
          token
        })

        this.repository.url = url
        this.repository.name = response.data.name
        this.repository.path = response.data.path

        // Load files
        await this.loadFiles()
      } catch (error) {
        console.error('Error cloning repository:', error)
        this.repository.error = 'Failed to clone repository. Please check the URL and try again.'
      } finally {
        this.repository.isLoading = false
      }
    },

    async loadFiles() {
      if (!this.repository.path) {
        return
      }

      try {
        const response = await axios.get(`http://localhost:8080/api/repository/files?path=${this.repository.path}`)
        this.fileStructure = response.data
      } catch (error) {
        console.error('Error loading files:', error)
        this.repository.error = 'Failed to load files from repository.'
      }
    }
  }
})
