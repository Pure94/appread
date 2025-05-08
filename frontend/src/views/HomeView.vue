<template>
  <div class="home">
    <h1>Welcome to AppRead</h1>
    <p>AppRead is a desktop application that helps you understand codebases using AI.</p>
    
    <div class="features">
      <div class="feature">
        <h2>Repository Analysis</h2>
        <p>Clone and analyze Git repositories to understand their structure and content.</p>
        <router-link to="/repository" class="btn">Get Started</router-link>
      </div>
      
      <div class="feature">
        <h2>AI-Powered Chat</h2>
        <p>Ask questions about the codebase and get intelligent answers based on the code.</p>
        <router-link to="/chat" class="btn">Start Chatting</router-link>
      </div>
      
      <div class="feature">
        <h2>Code Visualization</h2>
        <p>Generate diagrams to visualize the structure and relationships in the code.</p>
        <router-link to="/diagram" class="btn">View Diagrams</router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'HomeView'
})
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  text-align: center;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  color: #2c3e50;
}

.features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2rem;
  margin-top: 3rem;
}

.feature {
  flex: 1;
  min-width: 300px;
  background-color: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.feature:hover {
  transform: translateY(-5px);
}

h2 {
  color: #2c3e50;
  margin-bottom: 1rem;
}

.btn {
  display: inline-block;
  background-color: #2c3e50;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  margin-top: 1rem;
  transition: background-color 0.3s ease;
}

.btn:hover {
  background-color: #1e2b3a;
}
</style>