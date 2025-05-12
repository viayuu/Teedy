<template>
  <div class="container py-4">
    <h1 class="text-center mb-4">Teedy群组聊天</h1>
    
    <div class="row">
      <!-- 左侧群组列表 -->
      <div class="col-md-4 mb-4 mb-md-0">
        <GroupList @group-selected="onGroupSelected" />
      </div>
      
      <!-- 右侧聊天面板 -->
      <div class="col-md-8">
        <ChatPanel :group="selectedGroup" />
      </div>
    </div>
  
  </div>
</template>

<script>
import GroupList from '@/components/GroupList.vue';
import ChatPanel from '@/components/ChatPanel.vue';

export default {
  name: 'ChatView',
  components: {
    GroupList,
    ChatPanel
  },
  data() {
    return {
      selectedGroup: null,
      currentUser: null
    }
  },
  mounted() {
    // 从localStorage获取当前用户信息
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
    onGroupSelected(group) {
      this.selectedGroup = group;
    }
  }
}
</script> 