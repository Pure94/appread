<template>
  <div class="chat">
    <h1>AI-Powered Chat</h1>
    
    <div class="chat-container">
      <div class="chat-messages">
        <div v-if="messages.length === 0" class="empty-state">
          <p>No messages yet. Start a conversation about your codebase!</p>
        </div>
        <div v-else class="message-list">
          <div v-for="(message, index) in messages" :key="index" 
               :class="['message', message.sender === 'user' ? 'user-message' : 'ai-message']">
            <div class="message-content">
              <p>{{ message.text }}</p>
            </div>
            <div class="message-time">{{ message.timestamp }}</div>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <form @submit.prevent="sendMessage">
          <input 
            type="text" 
            v-model="newMessage" 
            placeholder="Ask a question about your code..."
            :disabled="isLoading"
          >
          <button type="submit" class="btn" :disabled="!newMessage.trim() || isLoading">
            {{ isLoading ? 'Sending...' : 'Send' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'

interface Message {
  text: string;
  sender: 'user' | 'ai';
  timestamp: string;
}

export default defineComponent({
  name: 'ChatView',
  
  setup() {
    const messages = ref<Message[]>([]);
    const newMessage = ref('');
    const isLoading = ref(false);
    
    const formatTime = (): string => {
      const now = new Date();
      return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };
    
    const sendMessage = async () => {
      if (!newMessage.value.trim()) return;
      
      // Add user message
      messages.value.push({
        text: newMessage.value,
        sender: 'user',
        timestamp: formatTime()
      });
      
      const userQuery = newMessage.value;
      newMessage.value = '';
      isLoading.value = true;
      
      try {
        // Simulate AI response (would be replaced with actual API call)
        setTimeout(() => {
          messages.value.push({
            text: `This is a placeholder response to: "${userQuery}"`,
            sender: 'ai',
            timestamp: formatTime()
          });
          isLoading.value = false;
        }, 1000);
      } catch (error) {
        console.error('Error getting AI response:', error);
        messages.value.push({
          text: 'Sorry, there was an error processing your request.',
          sender: 'ai',
          timestamp: formatTime()
        });
        isLoading.value = false;
      }
    };
    
    return {
      messages,
      newMessage,
      isLoading,
      sendMessage
    };
  }
});
</script>

<style scoped>
.chat {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  font-size: 2rem;
  margin-bottom: 2rem;
  color: #2c3e50;
}

.chat-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 70vh;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  background-color: #f8f9fa;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #95a5a6;
  font-style: italic;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  max-width: 70%;
  padding: 1rem;
  border-radius: 8px;
  position: relative;
}

.user-message {
  align-self: flex-end;
  background-color: #2c3e50;
  color: white;
}

.ai-message {
  align-self: flex-start;
  background-color: #ecf0f1;
  color: #2c3e50;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
  margin-top: 0.5rem;
  text-align: right;
}

.chat-input {
  padding: 1rem;
  border-top: 1px solid #ecf0f1;
}

.chat-input form {
  display: flex;
  gap: 0.5rem;
}

.chat-input input {
  flex: 1;
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
</style>