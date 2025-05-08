import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/repository',
    name: 'repository',
    // Route level code-splitting
    // this generates a separate chunk (repository.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "repository" */ '../views/RepositoryView.vue')
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import(/* webpackChunkName: "chat" */ '../views/ChatView.vue')
  },
  {
    path: '/diagram',
    name: 'diagram',
    component: () => import(/* webpackChunkName: "diagram" */ '../views/DiagramView.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router