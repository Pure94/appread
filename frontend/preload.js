// Preload script
const { contextBridge, ipcRenderer } = require('electron')

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // Get the app path
  getAppPath: () => ipcRenderer.invoke('get-app-path'),
  
  // Add more IPC methods as needed for communication with the main process
})