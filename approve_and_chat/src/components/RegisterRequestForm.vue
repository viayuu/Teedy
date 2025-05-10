<template>
  <div class="card mb-4">
    <div class="card-header bg-primary text-white">
      <h4>注册申请表单</h4>
    </div>
    <div class="card-body">
      <div v-if="successMessage" class="alert alert-success" role="alert">
        {{ successMessage }}
      </div>
      
      <div v-if="errorMessage" class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>
      
      <form @submit.prevent="submitRequest" v-if="!successMessage">
        <div class="mb-3">
          <label for="username" class="form-label">用户名</label>
          <input type="text" class="form-control" id="username" v-model="formData.username" required>
          <div class="form-text text-danger" v-if="validationErrors.username">{{ validationErrors.username }}</div>
        </div>
        
        <div class="mb-3">
          <label for="email" class="form-label">电子邮箱</label>
          <input type="email" class="form-control" id="email" v-model="formData.email" required>
          <div class="form-text text-danger" v-if="validationErrors.email">{{ validationErrors.email }}</div>
        </div>
        
        <div class="mb-3">
          <label for="password" class="form-label">密码</label>
          <input type="password" class="form-control" id="password" v-model="formData.password" required>
          <div class="form-text">密码长度至少8个字符</div>
          <div class="form-text text-danger" v-if="validationErrors.password">{{ validationErrors.password }}</div>
        </div>
        
        <div class="mb-3">
          <label for="confirmPassword" class="form-label">确认密码</label>
          <input type="password" class="form-control" id="confirmPassword" v-model="formData.confirmPassword" required>
          <div class="form-text text-danger" v-if="validationErrors.confirmPassword">{{ validationErrors.confirmPassword }}</div>
        </div>
        
        <div class="mb-3">
          <label for="storageQuota" class="form-label">存储配额 (字节)</label>
          <input type="number" class="form-control" id="storageQuota" v-model="formData.storageQuota" required min="1000000">
          <div class="form-text">建议值: 1000000000 (约1GB)</div>
          <div class="form-text text-danger" v-if="validationErrors.storageQuota">{{ validationErrors.storageQuota }}</div>
        </div>
        
        <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
          <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
          {{ isSubmitting ? '提交中...' : '提交申请' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'RegisterRequestForm',
  data() {
    return {
      formData: {
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        storageQuota: 1000000000 // 默认1GB
      },
      validationErrors: {},
      isSubmitting: false,
      successMessage: '',
      errorMessage: '',
      apiUrl: '/data-api/registrations' // 修改为相对路径，使用新的代理前缀
    }
  },
  methods: {
    validateForm() {
      this.validationErrors = {};
      let isValid = true;
      
      // 用户名验证
      if (this.formData.username.length < 3 || this.formData.username.length > 50) {
        this.validationErrors.username = '用户名长度必须在3-50个字符之间';
        isValid = false;
      }
      
      // 邮箱验证
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(this.formData.email)) {
        this.validationErrors.email = '请输入有效的电子邮箱地址';
        isValid = false;
      }
      
      // 密码验证
      if (this.formData.password.length < 8 || this.formData.password.length > 50) {
        this.validationErrors.password = '密码长度必须在8-50个字符之间';
        isValid = false;
      }
      
      // 确认密码
      if (this.formData.password !== this.formData.confirmPassword) {
        this.validationErrors.confirmPassword = '两次输入的密码不一致';
        isValid = false;
      }
      
      // 存储配额验证
      if (this.formData.storageQuota < 1000000) {
        this.validationErrors.storageQuota = '存储配额必须至少为1MB (1000000字节)';
        isValid = false;
      }
      
      return isValid;
    },
    
    async submitRequest() {
      // 表单验证
      if (!this.validateForm()) {
        return;
      }
      
      this.isSubmitting = true;
      this.errorMessage = '';
      
      try {
        // 准备要存储的注册请求数据
        const registrationRequest = {
          username: this.formData.username,
          email: this.formData.email,
          password: this.formData.password,
          storageQuota: this.formData.storageQuota,
          requestDate: new Date().toISOString()
        };
        
        // 发送到服务器保存
        await axios.post(this.apiUrl, registrationRequest);
        
        this.isSubmitting = false;
        this.successMessage = `申请已提交成功! 请等待管理员审批`;
        
        // 重置表单
        this.formData = {
          username: '',
          email: '',
          password: '',
          confirmPassword: '',
          storageQuota: 1000000000
        };
      } catch (error) {
        this.isSubmitting = false;
        this.errorMessage = `提交申请失败: ${error.response?.data?.error || error.message}`;
        console.error('Error submitting registration:', error);
      }
    }
  }
}
</script> 