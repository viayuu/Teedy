<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="text-center mb-0">Teedy群组聊天</h1>
      <div>
        <div v-if="loading" class="spinner-border spinner-border-sm text-primary me-2" role="status">
          <span class="visually-hidden">加载中...</span>
        </div>
        <button v-if="currentUser" @click="logout" class="btn btn-danger">
          <i class="bi bi-box-arrow-right me-1"></i> 退出并返回文档
        </button>
      </div>
    </div>
    
    <div class="row">
      <!-- 左侧群组列表 -->
      <div class="col-md-4 mb-4 mb-md-0">
        <GroupList @group-selected="onGroupSelected" ref="groupList" @user-loaded="onUserLoaded" />
      </div>
      
      <!-- 右侧聊天面板 -->
      <div class="col-md-8">
        <ChatPanel :group="selectedGroup" ref="chatPanel" @user-loaded="onUserLoaded" />
      </div>
    </div>
  
  </div>
</template>

<script>
import GroupList from '@/components/GroupList.vue';
import ChatPanel from '@/components/ChatPanel.vue';
import axios from 'axios';

export default {
  name: 'ChatView',
  components: {
    GroupList,
    ChatPanel
  },
  data() {
    return {
      selectedGroup: null,
      currentUser: null,
      checkUserInterval: null,
      loading: true,
      userCheckCount: 0
    }
  },
  created() {
    // 添加localStorage变化监听
    window.addEventListener('storage', this.checkUserStatus);
  },
  mounted() {
    // 立即检查用户状态
    this.checkUserStatus();
    
    // 设置定时检查，确保按钮状态正确
    this.checkUserInterval = setInterval(() => {
      this.checkUserStatus();
    }, 500); // 降低检查间隔，更快响应状态变化
    
    // 检查是否需要刷新页面（首次进入chat界面时）
    const hasRefreshed = localStorage.getItem('chatViewRefreshed');
    if (!hasRefreshed) {
      // 设置标记，避免刷新后再次刷新
      localStorage.setItem('chatViewRefreshed', 'true');
      
      // 使用setTimeout避免过早刷新，确保标记已经保存
      setTimeout(() => {
        console.log('首次进入聊天界面，自动刷新以确保组件正确初始化');
        window.location.reload();
      }, 100);
    } else {
      console.log('聊天界面已刷新过，无需再次刷新');
    }
  },
  beforeUnmount() {
    // 清除事件监听和定时器
    window.removeEventListener('storage', this.checkUserStatus);
    if (this.checkUserInterval) {
      clearInterval(this.checkUserInterval);
    }
  },
  methods: {
    onGroupSelected(group) {
      console.log('群组被选择:', group);
      this.selectedGroup = group;
      
      // 确保ChatPanel组件正确接收到群组信息
      if (this.$refs.chatPanel) {
        // 使用$nextTick确保DOM更新后再执行
        this.$nextTick(() => {
          // 手动触发ChatPanel的加载消息方法
          if (this.$refs.chatPanel.currentGroup !== group) {
            this.$refs.chatPanel.currentGroup = group;
            this.$refs.chatPanel.loadMessages();
          }
        });
      }
    },
    
    // 当ChatPanel加载用户信息时的回调
    onUserLoaded(user) {
      console.log('用户加载完成:', user);
      this.currentUser = user;
      
      // 如果已经有selectedGroup但ChatPanel可能还没加载它，再次触发选择
      if (this.selectedGroup && this.$refs.chatPanel) {
        this.$nextTick(() => {
          this.onGroupSelected(this.selectedGroup);
        });
      }
    },
    
    // 检查用户登录状态
    checkUserStatus() {
      this.userCheckCount++;
      
      const userJson = localStorage.getItem('currentUser');
      if (userJson) {
        try {
          const user = JSON.parse(userJson);
          if (user && user.username) {
            this.currentUser = user;
            this.loading = false;
            
            // 找到用户后，可以减少检查频率
            if (this.checkUserInterval) {
              clearInterval(this.checkUserInterval);
              this.checkUserInterval = setInterval(() => {
                this.checkUserStatus();
              }, 3000); // 找到用户后降低频率
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e);
        }
      } else {
        this.currentUser = null;
      }
      
      // 如果连续检查超过10次仍未找到用户，停止加载状态
      if (this.userCheckCount > 10) {
        this.loading = false;
      }
    },
    
    async logout() {
      if (this.currentUser && this.currentUser.username) {
        try {
          // 通知聊天服务器用户下线
          await axios.post('/chat-api/notify/logout', {
            username: this.currentUser.username
          });
          
          // 清除本地存储的用户信息
          localStorage.removeItem('currentUser');
          localStorage.removeItem('currentChatGroup');
          // 清除刷新标记，确保下次登录后能正确刷新
          localStorage.removeItem('chatViewRefreshed');
          this.currentUser = null;
          
          // 跳转到Teedy文档系统
          window.location.href = 'http://localhost:8080/docs-web/src/#/document';
        } catch (error) {
          console.error('登出失败:', error);
          alert('登出过程中出现错误，请重试');
        }
      }
    }
  }
}
</script> 