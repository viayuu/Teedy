import { createRouter, createWebHistory } from 'vue-router'
import RegisterRequestView from '../views/RegisterRequestView.vue'
import ChatView from '../views/ChatView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: RegisterRequestView
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatView
    }
  ]
})

export default router 