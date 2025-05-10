<template>
  <div class="card mb-4">
    <div class="card-header bg-primary text-white">
      <h4>待审批注册申请</h4>
    </div>
    <div class="card-body">
      <div v-if="statusMessage" :class="['alert', statusMessage.type]" role="alert">
        {{ statusMessage.text }}
      </div>
      
      <div class="mb-3">
        <button class="btn btn-outline-secondary" type="button" @click="loadPendingRegistrations">
          <i class="bi bi-arrow-clockwise"></i> 刷新列表
        </button>
      </div>
      
      <hr>
      
      <div v-if="pendingRegistrations.length === 0" class="alert alert-info">
        当前没有待审批的注册申请
      </div>
      
      <div v-else>
        <div class="table-responsive">
          <table class="table table-bordered">
            <thead class="table-light">
              <tr>
                <th>用户名</th>
                <th>电子邮箱</th>
                <th>存储配额</th>
                <th>申请时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(registration, index) in pendingRegistrations" :key="index">
                <td>{{ registration.username }}</td>
                <td>{{ registration.email }}</td>
                <td>{{ formatStorage(registration.storageQuota) }}</td>
                <td>{{ formatDate(registration.requestDate) }}</td>
                <td>
                  <div class="d-flex gap-2">
                    <button class="btn btn-success btn-sm" 
                            @click="approveRegistration(registration, index)"
                            :disabled="processingIndex === index">
                      <span v-if="processingIndex === index && isApproving" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      同意
                    </button>
                    <button class="btn btn-danger btn-sm" 
                            @click="rejectRegistration(registration)"
                            :disabled="processingIndex === index">
                      <span v-if="processingIndex === index && !isApproving" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      拒绝
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ApproveRegistrations',
  data() {
    return {
      pendingRegistrations: [],
      processingIndex: null,
      isApproving: false,
      statusMessage: null,
      apiUrl: '/data-api/registrations' // 修改为相对路径，使用新的代理前缀
    }
  },
  mounted() {
    this.loadPendingRegistrations();
  },
  methods: {
    // 从服务器加载数据
    async loadPendingRegistrations() {
      try {
        const response = await axios.get(this.apiUrl);
        this.pendingRegistrations = response.data;
        this.setStatusMessage('已从服务器加载最新数据', 'alert-success');
      } catch (error) {
        console.error('Error loading registrations:', error);
        this.setStatusMessage(
          `加载注册申请失败: ${error.response?.data?.error || error.message}`, 
          'alert-danger'
        );
      }
    },
    
    formatStorage(bytes) {
      if (bytes >= 1000000000) {
        return (bytes / 1000000000).toFixed(2) + ' GB';
      } else if (bytes >= 1000000) {
        return (bytes / 1000000).toFixed(2) + ' MB';
      } else if (bytes >= 1000) {
        return (bytes / 1000).toFixed(2) + ' KB';
      }
      return bytes + ' B';
    },
    
    formatDate(dateString) {
      try {
        const date = new Date(dateString);
        return date.toLocaleString();
      } catch(e) {
        return '未知时间';
      }
    },
    
    setStatusMessage(text, type = 'alert-success', duration = 5000) {
      this.statusMessage = { text, type };
      if (duration > 0) {
        setTimeout(() => {
          this.statusMessage = null;
        }, duration);
      }
    },
    
    async approveRegistration(registration, index) {
      this.processingIndex = index;
      this.isApproving = true;
      
      try {
        // 使用URLSearchParams构建表单数据，符合application/x-www-form-urlencoded格式
        const params = new URLSearchParams();
        params.append('username', registration.username);
        params.append('password', registration.password);
        params.append('email', registration.email);
        params.append('storage_quota', registration.storageQuota);
        
        // 调用API创建用户
        await axios.put('/api/user', params, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          withCredentials: true // 确保发送Cookie用于身份验证
        });
        
        // 删除服务器上的申请
        await this.removeRegistration(registration.username);
        
        this.setStatusMessage(`用户 ${registration.username} 已成功创建`);
        await this.loadPendingRegistrations(); // 重新加载列表
      } catch (error) {
        console.error('Create user error:', error);
        this.setStatusMessage(
          `创建用户失败: ${error.response?.data?.message || error.message}`, 
          'alert-danger'
        );
      } finally {
        this.processingIndex = null;
        this.isApproving = false;
      }
    },
    
    async rejectRegistration(registration) {
      const index = this.pendingRegistrations.findIndex(r => r.username === registration.username);
      this.processingIndex = index;
      this.isApproving = false;
      
      try {
        // 从服务器删除申请
        await this.removeRegistration(registration.username);
        
        this.setStatusMessage(`已拒绝用户 ${registration.username} 的注册申请`);
        await this.loadPendingRegistrations(); // 重新加载列表
      } catch (error) {
        console.error('Reject registration error:', error);
        this.setStatusMessage(
          `拒绝注册申请失败: ${error.response?.data?.error || error.message}`, 
          'alert-danger'
        );
      } finally {
        this.processingIndex = null;
      }
    },
    
    async removeRegistration(username) {
      // 调用服务器API删除注册申请
      await axios.delete(`${this.apiUrl}/${username}`);
    }
  }
}
</script> 