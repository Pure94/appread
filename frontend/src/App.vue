<template>
  <div id="app">
    <header>
      <h1>AppRead</h1>
    </header>
    
    <main>
      <router-view />
    </main>
    
    <footer>
      <p>&copy; 2023 AppRead</p>
    </footer>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'App'
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
}

#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

header {
  background-color: #2c3e50;
  color: white;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

main {
  flex: 1;
  padding: 1rem;
}

footer {
  background-color: #2c3e50;
  color: white;
  padding: 1rem;
  text-align: center;
}
</style>