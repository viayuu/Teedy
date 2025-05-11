<template>
  <div class="card mb-4">
    <div class="card-header bg-primary text-white">
      <h4>群组列表</h4>
    </div>
    <div class="card-body">
      <div v-if="loading" class="d-flex justify-content-center my-3">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">加载中...</span>
        </div>
      </div>
      
      <div v-else-if="error" class="alert alert-danger">
        {{ error }}
      </div>
      
      <div v-else>
        <div v-if="userGroups.length === 0" class="alert alert-info">
          您当前不属于任何群组。
        </div>
        
        <div v-else>
          <p>选择一个群组进行聊天:</p>
          
          <div class="list-group">
            <button 
              v-for="group in userGroups" 
              :key="group.id"
              class="list-group-item list-group-item-action"
              :class="{'active': selectedGroup && selectedGroup.id === group.id}"
              @click="selectGroup(group)"
            >
              <div class="d-flex w-100 justify-content-between">
                <h5 class="mb-1">{{ group.name }}</h5>
                <small>{{ group.members.length }} 位成员</small>
              </div>
              <div class="mb-1">
                <span 
                  v-for="member in group.members" 
                  :key="typeof member === 'string' ? member : member.username"
                  class="badge me-1"
                  :class="isUserOnline(typeof member === 'string' ? member : member.username) ? 'bg-success' : 'bg-secondary'"
                >
                  {{ typeof member === 'string' ? member : member.username }}{{ isCurrentUser(typeof member === 'string' ? member : member.username) ? ' (您)' : '' }}
                </span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'GroupList',
  data() {
    return {
      loading: true,
      error: null,
      groups: [],
      userGroups: [],
      onlineUsers: [],
      currentUser: null,
      selectedGroup: null,
      refreshInterval: null,
      debug: true
    }
  },
  async mounted() {
    await this.initialize();
  },
  beforeUnmount() {
    // 清除定时器
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  },
  methods: {
    async initialize() {
      this.loading = true;
      this.error = null;

      try {
        // 尝试从Teedy获取当前用户信息
        const userResponse = await axios.get('/api/user');
        if (userResponse.data) {
          this.currentUser = {
            username: userResponse.data.username,
            email: userResponse.data.email || ''
          };
          localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
        } else {
          throw new Error('无法获取用户信息');
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        // 尝试从localStorage获取上次存储的用户信息
        const storedUser = localStorage.getItem('currentUser');
        if (storedUser) {
          try {
            this.currentUser = JSON.parse(storedUser);
          } catch (e) {
            this.error = '无法获取用户信息，请确保已登录到Teedy';
            this.loading = false;
            return;
          }
        } else {
          this.error = '无法获取用户信息，请确保已登录到Teedy';
          this.loading = false;
          return;
        }
      }
      
      // 直接加载数据
      await this.loadData();
      
      // 设置刷新定时器
      this.refreshInterval = setInterval(async () => {
        await this.refreshOnlineUsers();
      }, 1000); // 每10秒刷新一次在线用户
      
      this.loading = false;
    },

    async loadData() {
      try {
        this.loading = true;
        this.error = null;
        
        // 1. 获取所有群组和用户
        await this.loadGroups();
        
        // 2. 获取在线用户
        await this.refreshOnlineUsers();
        
        // 3. 初始化聊天记录文件
        await this.initGroupChats();
        
        // 4. 如果有用户组，选择第一个
        if (this.userGroups.length > 0) {
          this.selectGroup(this.userGroups[0]);
        }
        
        this.loading = false;
      } catch (error) {
        console.error('加载数据失败:', error);
        this.error = '无法加载群组数据';
        this.loading = false;
      }
    },
    
    async loadGroups() {
      try {
        if (this.debug) console.log('尝试加载群组数据');
        
        // 直接从Teedy获取群组
        if (this.debug) console.log('发送请求: /api/group');
        const response = await axios.get('/api/group?sort_column=0&asc=true');
        if (this.debug) console.log('Teedy API响应:', response.data);
        
        // 尝试多种可能的API返回格式
        if (Array.isArray(response.data)) {
          // 如果直接返回数组
          this.groups = response.data;
        } else if (response.data.groups && Array.isArray(response.data.groups)) {
          // 如果返回{groups: [...]}
          this.groups = response.data.groups;
        } else if (typeof response.data === 'object') {
          // 如果返回的是其他对象格式，尝试转换为数组
          this.groups = Object.values(response.data).filter(item => 
            typeof item === 'object' && item !== null
          );
        } else {
          // 无法识别的格式，使用空数组
          this.groups = [];
        }
        
        if (this.debug) console.log('解析后的群组:', this.groups);
        
        // 获取每个群组的详细信息
        for (const group of this.groups) {
          try {
            // 确保group有id属性和name属性
            if (!group.id && group.name) {
              group.id = group.name; // 使用name作为id
            }
            
            const groupId = group.id || group.name || group.groupId;
            if (!groupId) {
              console.error('群组对象缺少id:', group);
              continue;
            }
            
            // 首先检查群组是否已有members属性
            if (!group.members || !Array.isArray(group.members) || group.members.length === 0) {
              if (this.debug) console.log(`发送请求: /api/group/${encodeURIComponent(groupId)}`);
              const groupDetailResponse = await axios.get(`/api/group/${encodeURIComponent(groupId)}`);
              if (this.debug) console.log(`群组 ${groupId} 详情:`, groupDetailResponse.data);
              
              // 尝试多种可能的详情格式
              if (groupDetailResponse.data.members && Array.isArray(groupDetailResponse.data.members)) {
                group.members = groupDetailResponse.data.members;
              } else if (Array.isArray(groupDetailResponse.data)) {
                group.members = groupDetailResponse.data;
              } else if (groupDetailResponse.data.users && Array.isArray(groupDetailResponse.data.users)) {
                group.members = groupDetailResponse.data.users;
              } else {
                group.members = [];
              }
              
              // 确保group有name属性
              if (!group.name && groupDetailResponse.data.name) {
                group.name = groupDetailResponse.data.name;
              }
            }
            
            console.log(`群组 ${group.name} 的成员:`, group.members);
          } catch (error) {
            console.error(`获取群组详情失败:`, error);
            group.members = group.members || [];
          }
        }
        
        // 筛选出当前用户所在的群组
        this.filterUserGroups();
        if (this.debug) {
          console.log('筛选后的用户群组:', this.userGroups);
          console.log('当前用户:', this.currentUser);
          
          // 输出详细的筛选日志
          if (this.userGroups.length === 0 && this.groups.length > 0) {
            console.log('没有找到当前用户所在的群组，详细筛选记录:');
            this.groups.forEach(group => {
              let hasUser = false;
              
              if (Array.isArray(group.members)) {
                // 处理成员是字符串数组的情况
                if (typeof group.members[0] === 'string') {
                  hasUser = group.members.includes(this.currentUser?.username);
                  console.log(`群组 ${group.name} (id: ${group.id}): ${hasUser ? '包含当前用户' : '不包含当前用户'}`);
                  console.log('成员:', group.members.join(', '));
                } 
                // 处理成员是对象数组的情况
                else if (typeof group.members[0] === 'object') {
                  hasUser = group.members.some(member => member.username === this.currentUser?.username);
                  console.log(`群组 ${group.name} (id: ${group.id}): ${hasUser ? '包含当前用户' : '不包含当前用户'}`);
                  console.log('成员:', group.members.map(m => m.username).join(', '));
                }
                else {
                  console.log(`群组 ${group.name} (id: ${group.id}): 成员格式无法识别`);
                  console.log('成员:', group.members);
                }
              } else {
                console.log(`群组 ${group.name} (id: ${group.id}): 成员不是数组`);
              }
            });
          }
        }
        
      } catch (error) {
        console.error('加载群组失败:', error);
        throw error;
      }
    },
    
    async refreshOnlineUsers() {
      try {
        const response = await axios.get('/chat-api/chat/online-users');
        this.onlineUsers = response.data;
        
        // 同时通知聊天服务器当前用户在线
        if (this.currentUser && this.currentUser.username) {
          await axios.post('/chat-api/notify/login', {
            username: this.currentUser.username
          });
        }
      } catch (error) {
        console.error('获取在线用户失败:', error);
      }
    },
    
    filterUserGroups() {
      if (!this.currentUser || !this.currentUser.username) {
        this.userGroups = [];
        return;
      }
      
      // 筛选出当前用户所在的群组
      this.userGroups = this.groups.filter(group => {
        if (!Array.isArray(group.members)) {
          return false;
        }
        
        // 根据成员数组的类型进行不同的比较
        if (group.members.length > 0) {
          if (typeof group.members[0] === 'string') {
            // 如果成员是字符串数组，直接比较用户名
            return group.members.includes(this.currentUser.username);
          } else if (typeof group.members[0] === 'object') {
            // 如果成员是对象数组，比较username属性
            return group.members.some(member => member.username === this.currentUser.username);
          }
        }
        return false;
      });
    },
    
    async initGroupChats() {
      // 为每个用户所在的群组初始化聊天记录文件
      try {
        if (this.userGroups.length === 0) {
          return;
        }
        
        const groupNames = this.userGroups.map(group => group.name);
        console.log('初始化群组聊天记录:', groupNames);
        
        const response = await axios.post('/chat-api/chat/groups/init', { groups: groupNames });
        if (this.debug) console.log('初始化群组聊天返回:', response.data);
      } catch (error) {
        console.error('初始化群组聊天失败:', error);
      }
    },
    
    selectGroup(group) {
      this.selectedGroup = group;
      
      // 存储当前选中的群组到localStorage
      localStorage.setItem('currentChatGroup', JSON.stringify({
        id: group.id,
        name: group.name
      }));
      
      // 发出事件通知父组件
      this.$emit('group-selected', group);
    },
    
    isUserOnline(username) {
      return this.onlineUsers.includes(username);
    },
    
    isCurrentUser(username) {
      return this.currentUser && this.currentUser.username === username;
    }
  }
}
</script> 