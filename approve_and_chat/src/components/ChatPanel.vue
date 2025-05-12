<template>
  <div class="card">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
      <h4>{{ currentGroup ? currentGroup.name : '群组聊天' }}</h4>
      <div v-if="currentGroup">
        <span class="badge bg-light text-dark">{{ currentGroup.members ? currentGroup.members.length : 0 }} 位成员</span>
      </div>
    </div>
    
    <div class="card-body p-0">
      <!-- 聊天记录区域 -->
      <div 
        ref="chatLog"
        class="chat-messages p-3" 
        style="height: 400px; overflow-y: auto;"
      >
        <div v-if="loading" class="d-flex justify-content-center my-3">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">加载中...</span>
          </div>
        </div>
        
        <div v-else-if="error" class="alert alert-danger mx-3 my-3">
          <strong>错误:</strong> {{ error }}
          <div class="mt-2">
            <button @click="retryLoadMessages" class="btn btn-sm btn-outline-danger">
              <i class="bi bi-arrow-clockwise"></i> 重试
            </button>
          </div>
        </div>
        
        <div v-else-if="!currentGroup" class="alert alert-info mx-3 my-3">
          请先选择一个群组进行聊天
        </div>
        
        <div v-else-if="chatMessages.length === 0" class="text-center text-muted my-5">
          还没有任何消息，开始聊天吧！
        </div>
        
        <div v-else>
          <div 
            v-for="message in chatMessages" 
            :key="message.id"
            class="message mb-3"
            :class="{'message-self': isCurrentUser(message.username)}"
          >
            <div class="message-header d-flex justify-content-between">
              <strong>{{ message.username }}</strong>
              <small class="text-muted">{{ formatTime(message.timestamp) }}</small>
            </div>
            <div 
              class="message-body p-2 rounded"
              :class="{'bg-primary text-white': isCurrentUser(message.username), 'bg-light': !isCurrentUser(message.username)}"
            >
              {{ message.message }}
            </div>
          </div>
        </div>
      </div>
      
      <!-- 消息输入区域 -->
      <div class="message-input p-3 border-top">
        <form @submit.prevent="sendMessage">
          <div v-if="error" class="alert alert-danger mb-3">{{ error }}</div>
          
          <div class="input-group">
            <input 
              type="text" 
              class="form-control" 
              v-model="newMessage" 
              placeholder="输入消息..." 
              :disabled="!currentGroup || sending || !currentUser"
            >
            <button 
              class="btn btn-primary" 
              type="submit" 
              :disabled="!currentGroup || !newMessage.trim() || sending || !currentUser"
            >
              <span v-if="sending" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
              发送
            </button>
          </div>
          
          <div v-if="!currentUser" class="text-danger small mt-2">
            <i class="bi bi-exclamation-triangle-fill"></i> 用户信息未加载，请刷新页面或重新登录
          </div>
          <div v-if="!currentGroup" class="text-warning small mt-2">
            <i class="bi bi-info-circle-fill"></i> 请先从左侧选择一个群组
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ChatPanel',
  props: {
    group: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      currentGroup: null,
      currentUser: null,
      chatMessages: [],
      newMessage: '',
      loading: false,
      sending: false,
      error: null,
      refreshInterval: null
    }
  },
  watch: {
    group(newGroup) {
      console.log('ChatPanel收到新群组:', newGroup);
      if (newGroup) {
        this.currentGroup = newGroup;
        this.loadMessages();
      }
    }
  },
  async mounted() {
    console.log('ChatPanel mounted, 接收到的group:', this.group);
    
    // 获取当前用户信息
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      try {
        this.currentUser = JSON.parse(userJson);
        // 通知父组件用户已加载
        this.$emit('user-loaded', this.currentUser);
      } catch (e) {
        console.error('解析用户信息失败:', e);
        this.error = '无法获取用户信息，请重新登录';
      }
    }
    
    // 如果有传入的group，则使用该group
    if (this.group) {
      console.log('使用props传入的群组:', this.group);
      this.currentGroup = this.group;
      await this.loadMessages();
    } else {
      // 否则，尝试从localStorage获取上次选择的群组
      const groupJson = localStorage.getItem('currentChatGroup');
      if (groupJson) {
        try {
          const savedGroup = JSON.parse(groupJson);
          console.log('使用localStorage中的群组:', savedGroup);
          this.currentGroup = savedGroup;
          await this.loadMessages();
        } catch (e) {
          console.error('解析群组信息失败:', e);
        }
      }
    }
    
    // 设置定时刷新聊天记录
    this.refreshInterval = setInterval(() => {
      if (this.currentGroup) {
        this.refreshMessages();
      }
    }, 1000); // 每1秒刷新一次
    
    // 页面卸载前，通知服务器用户下线
    window.addEventListener('beforeunload', this.notifyLogout);
  },
  beforeUnmount() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    this.notifyLogout();
    window.removeEventListener('beforeunload', this.notifyLogout);
  },
  updated() {
    // 消息更新后滚动到底部
    this.scrollToBottom();
  },
  methods: {
    async loadMessages() {
      if (!this.currentGroup) return;
      
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.get(`/chat-api/chat/groups/${this.currentGroup.name}/log`);
        this.chatMessages = response.data;
        this.loading = false;
        
        // 滚动到最新消息
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      } catch (error) {
        console.error('加载消息失败:', error);
        this.error = '无法加载聊天记录: ' + (error.response?.data?.error || error.message);
        this.loading = false;
      }
    },
    
    async refreshMessages() {
      if (!this.currentGroup) return;
      
      try {
        const response = await axios.get(`/chat-api/chat/groups/${this.currentGroup.name}/log`);
        // 检查是否有新消息
        if (response.data.length !== this.chatMessages.length) {
          // 更新消息列表并滚动到底部
          this.chatMessages = response.data;
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      } catch (error) {
        console.error('刷新消息失败:', error);
      }
    },
    
    async sendMessage() {
      // 详细检查每个条件，以便更好地调试
      if (!this.currentGroup) {
        console.error('发送消息失败: 当前没有选择群组');
        this.error = '请先选择一个群组';
        return;
      }
      
      if (!this.newMessage.trim()) {
        console.error('发送消息失败: 消息内容为空');
        return;
      }
      
      if (!this.currentUser) {
        console.error('发送消息失败: 当前用户信息缺失');
        this.error = '用户信息未加载，请刷新页面或重新登录';
        
        // 尝试重新获取用户信息
        const userJson = localStorage.getItem('currentUser');
        if (userJson) {
          try {
            this.currentUser = JSON.parse(userJson);
            console.log('重新加载用户信息成功:', this.currentUser);
          } catch (e) {
            console.error('重新解析用户信息失败:', e);
            return;
          }
        } else {
          return;
        }
      }
      
      this.sending = true;
      this.error = null;
      
      try {
        console.log('正在发送消息:', {
          groupName: this.currentGroup.name,
          username: this.currentUser.username,
          message: this.newMessage.trim()
        });
        
        await axios.post('/chat-api/chat/groups/append-message', {
          groupName: this.currentGroup.name,
          username: this.currentUser.username,
          message: this.newMessage.trim()
        });
        
        // 清空输入框并刷新消息
        this.newMessage = '';
        await this.refreshMessages();
        
        this.sending = false;
      } catch (error) {
        console.error('发送消息失败:', error);
        this.error = '无法发送消息: ' + (error.response?.data?.error || error.message);
        this.sending = false;
      }
    },
    
    async notifyLogout() {
      if (this.currentUser && this.currentUser.username) {
        try {
          // 使用信号来避免重复调用
          if (window.logoutInProgress) return;
          window.logoutInProgress = true;
          
          await axios.post('/chat-api/notify/logout', {
            username: this.currentUser.username
          });
          
          console.log(`用户 ${this.currentUser.username} 已成功登出`);
        } catch (error) {
          console.error('通知登出失败:', error);
        } finally {
          window.logoutInProgress = false;
        }
      }
    },
    
    isCurrentUser(username) {
      return this.currentUser && this.currentUser.username === username;
    },
    
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },
    
    scrollToBottom() {
      const chatLog = this.$refs.chatLog;
      if (chatLog) {
        chatLog.scrollTop = chatLog.scrollHeight;
      }
    },
    
    async retryLoadMessages() {
      this.error = null;
      
      // 尝试重新加载消息
      if (this.currentGroup) {
        await this.loadMessages();
      } 
      // 如果没有currentGroup但有props.group，则使用它
      else if (this.group) {
        this.currentGroup = this.group;
        await this.loadMessages();
      }
      // 如果还是没有，尝试从localStorage获取
      else {
        const groupJson = localStorage.getItem('currentChatGroup');
        if (groupJson) {
          try {
            const savedGroup = JSON.parse(groupJson);
            this.currentGroup = savedGroup;
            await this.loadMessages();
          } catch (e) {
            console.error('重试时解析群组信息失败:', e);
            this.error = '无法加载群组信息，请重新选择群组';
          }
        } else {
          this.error = '没有可用的群组信息，请从左侧选择一个群组';
        }
      }
    }
  }
}
</script>

<style scoped>
.message-self .message-body {
  margin-left: auto;
  max-width: 80%;
  width: fit-content;
}

.message-body {
  max-width: 80%;
  width: fit-content;
}

.chat-messages {
  background-color: #f8f9fa;
}
</style> 