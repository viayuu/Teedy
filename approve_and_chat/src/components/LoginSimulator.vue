<template>
  <div class="card mb-4">
    <div class="card-header bg-primary text-white">
      <h4>模拟用户登录</h4>
      <small class="text-white-50">（仅用于测试群组聊天功能）</small>
    </div>
    <div class="card-body">
      <div v-if="currentUser" class="alert alert-success">
        <strong>当前登录用户:</strong> {{ currentUser.username }}
        <button @click="logout" class="btn btn-sm btn-outline-danger ms-3">退出登录</button>
      </div>
      <form v-else @submit.prevent="login">
        <div class="mb-3">
          <label for="username" class="form-label">用户名</label>
          <input type="text" class="form-control" id="username" v-model="username" required>
        </div>
        <div class="mb-3">
          <label for="email" class="form-label">电子邮箱</label>
          <input type="email" class="form-control" id="email" v-model="email" required>
        </div>
        <button type="submit" class="btn btn-primary">模拟登录</button>
      </form>
      
      <div class="mt-3 text-muted">
        <p class="small">
          注意: 此组件仅用于测试。在实际环境中，用户登录应该通过Teedy主系统进行。
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'LoginSimulator',
  data() {
    return {
      username: '',
      email: '',
      currentUser: null
    }
  },
  mounted() {
    // 检查localStorage中是否有用户信息
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      try {
        this.currentUser = JSON.parse(userJson);
      } catch (e) {
        console.error('解析用户信息失败:', e);
      }
    }
  },
  methods: {
    async login() {
      if (!this.username || !this.email) return;
      
      // 创建用户对象
      const user = {
        username: this.username,
        email: this.email
      };
      
      // 保存到localStorage
      localStorage.setItem('currentUser', JSON.stringify(user));
      this.currentUser = user;
      
      // 通知聊天服务器用户上线
      try {
        await axios.post('/chat-api/notify/login', { username: this.username });
      } catch (error) {
        console.error('通知登录失败:', error);
      }
      
      // 清空表单
      this.username = '';
      this.email = '';
      
      // 刷新页面以重新加载组件和数据
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    },
    
    async logout() {
      if (!this.currentUser) return;
      
      // 通知聊天服务器用户下线
      try {
        await axios.post('/chat-api/notify/logout', { username: this.currentUser.username });
      } catch (error) {
        console.error('通知登出失败:', error);
      }
      
      // 从localStorage删除用户信息
      localStorage.removeItem('currentUser');
      this.currentUser = null;
      
      // 刷新页面以重新加载组件和数据
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    }
  }
}
</script> 