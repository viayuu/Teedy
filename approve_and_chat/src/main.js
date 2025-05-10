import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// 导入Bootstrap样式
import 'bootstrap/dist/css/bootstrap.css'

// 配置Axios默认设置
axios.defaults.withCredentials = true // 允许跨域请求带上凭证

const app = createApp(App)
app.use(router)
app.mount('#app') 