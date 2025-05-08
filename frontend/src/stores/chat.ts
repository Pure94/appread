import { defineStore } from 'pinia'
import axios from 'axios'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

interface ChatState {
  conversationId: string
  messages: Message[]
  isLoading: boolean
  error: string | null
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversationId: '',
    messages: [],
    isLoading: false,
    error: null
  }),
  
  getters: {
    conversationHistory: (state) => state.messages
  },
  
  actions: {
    async sendMessage(content: string) {
      if (!content.trim()) {
        return
      }
      
      // Add user message to the conversation
      const userMessage: Message = {
        id: Date.now().toString(),
        role: 'user',
        content,
        timestamp: Date.now()
      }
      
      this.messages.push(userMessage)
      this.isLoading = true
      this.error = null
      
      try {
        // Send the message to the backend
        const response = await axios.post('http://localhost:8080/api/chat/message', {
          conversationId: this.conversationId || undefined,
          message: content
        })
        
        // Update conversation ID if it's a new conversation
        if (!this.conversationId && response.data.conversationId) {
          this.conversationId = response.data.conversationId
        }
        
        // Add assistant message to the conversation
        const assistantMessage: Message = {
          id: Date.now().toString(),
          role: 'assistant',
          content: response.data.response,
          timestamp: Date.now()
        }
        
        this.messages.push(assistantMessage)
      } catch (error) {
        console.error('Error sending message:', error)
        this.error = 'Failed to send message. Please try again.'
      } finally {
        this.isLoading = false
      }
    },
    
    clearConversation() {
      this.messages = []
      this.error = null
      
      // Optionally, notify the backend to clear the conversation
      if (this.conversationId) {
        axios.delete(`http://localhost:8080/api/chat/conversation/${this.conversationId}`)
          .catch(error => console.error('Error clearing conversation:', error))
      }
      
      // Generate a new conversation ID
      this.conversationId = ''
    }
  }
})