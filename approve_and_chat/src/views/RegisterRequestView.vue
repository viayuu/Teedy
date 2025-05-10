<template>
  <div>
    <div v-if="isLoading" class="text-center">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p>正在加载用户信息...</p>
    </div>
    
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
    </div>
    
    <div v-else>
      <!-- 访客显示申请表单 -->
      <RegisterRequestForm v-if="isGuest" />
      
      <!-- 管理员显示审批列表 -->
      <ApproveRegistrations v-else-if="isAdmin" />
      
      <!-- 其他用户显示无权限信息 -->
      <div v-else class="alert alert-warning" role="alert">
        您当前的账户既不是访客也不是管理员，无法使用此功能。
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import RegisterRequestForm from '../components/RegisterRequestForm.vue';
import ApproveRegistrations from '../components/ApproveRegistrations.vue';

export default {
  name: 'RegisterRequestView',
  components: {
    RegisterRequestForm,
    ApproveRegistrations
  },
  data() {
    return {
      isLoading: true,
      user: null,
      errorMessage: null
    }
  },
  computed: {
    isGuest() {
      return this.user?.username === 'guest';
    },
    isAdmin() {
      return this.user?.base_functions?.includes('ADMIN');
    }
  },
  mounted() {
    this.getCurrentUser();
  },
  methods: {
    getCurrentUser() {
      axios.get('/api/user')
        .then(response => {
          this.isLoading = false;
          
          // 检查是否是匿名用户
          if (response.data.anonymous) {
            this.errorMessage = '您需要先登录Teedy系统才能使用此功能。';
            return;
          }
          
          this.user = response.data;
        })
        .catch(error => {
          this.isLoading = false;
          this.errorMessage = '获取用户信息失败: ' + (error.response?.data?.message || error.message);
          console.error('Error fetching user data:', error);
        });
    }
  }
}
</script> 