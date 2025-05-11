import express from 'express';
import cors from 'cors';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { createServer } from 'http';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const PORT = 3002;

// 定义数据存储路径
const DATA_DIR = join(__dirname, 'data');
const CHAT_LOGS_DIR = join(DATA_DIR, 'chat_logs');
const GROUP_CHATS_DIR = join(CHAT_LOGS_DIR, 'group_chats');
const ONLINE_USERS_FILE = join(DATA_DIR, 'online_users.json');

// 确保必要的目录存在
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR);
}
if (!fs.existsSync(CHAT_LOGS_DIR)) {
  fs.mkdirSync(CHAT_LOGS_DIR);
}
if (!fs.existsSync(GROUP_CHATS_DIR)) {
  fs.mkdirSync(GROUP_CHATS_DIR);
}

// 确保在线用户文件存在
if (!fs.existsSync(ONLINE_USERS_FILE)) {
  fs.writeFileSync(ONLINE_USERS_FILE, '[]', 'utf8');
}

// 中间件
app.use(cors()); // 允许跨域请求
app.use(express.json()); // 解析JSON请求体

// [辅助后端步骤 2.1] 用户在线状态管理 API
app.post('/notify/login', (req, res) => {
  try {
    const { username } = req.body;
    if (!username) {
      return res.status(400).json({ error: '用户名不能为空' });
    }

    // 读取当前在线用户
    const data = fs.readFileSync(ONLINE_USERS_FILE, 'utf8');
    let onlineUsers = JSON.parse(data);
    
    // 添加用户到在线列表(如果不存在)
    if (!onlineUsers.includes(username)) {
      onlineUsers.push(username);
      // 保存更新后的在线用户列表
      fs.writeFileSync(ONLINE_USERS_FILE, JSON.stringify(onlineUsers, null, 2), 'utf8');
    }
    
    res.json({ success: true, message: `用户 ${username} 已标记为在线` });
  } catch (error) {
    console.error('登录通知错误:', error);
    res.status(500).json({ error: '处理登录通知失败' });
  }
});

app.post('/notify/logout', (req, res) => {
  try {
    const { username } = req.body;
    if (!username) {
      return res.status(400).json({ error: '用户名不能为空' });
    }

    // 读取当前在线用户
    const data = fs.readFileSync(ONLINE_USERS_FILE, 'utf8');
    let onlineUsers = JSON.parse(data);
    
    // 从在线列表中移除用户
    onlineUsers = onlineUsers.filter(user => user !== username);
    
    // 保存更新后的在线用户列表
    fs.writeFileSync(ONLINE_USERS_FILE, JSON.stringify(onlineUsers, null, 2), 'utf8');
    
    res.json({ success: true, message: `用户 ${username} 已标记为离线` });
  } catch (error) {
    console.error('登出通知错误:', error);
    res.status(500).json({ error: '处理登出通知失败' });
  }
});

app.get('/chat/online-users', (req, res) => {
  try {
    // 读取并返回在线用户列表
    const data = fs.readFileSync(ONLINE_USERS_FILE, 'utf8');
    const onlineUsers = JSON.parse(data);
    
    res.json(onlineUsers);
  } catch (error) {
    console.error('获取在线用户错误:', error);
    res.status(500).json({ error: '获取在线用户列表失败' });
  }
});

// [辅助后端步骤 2.2] 群聊文件初始化 API
app.post('/chat/groups/init', (req, res) => {
  try {
    const { groups } = req.body;
    if (!Array.isArray(groups)) {
      return res.status(400).json({ error: '群组列表必须是数组' });
    }

    const results = {};
    
    // 为每个群组创建聊天记录文件(如果不存在)
    for (const group of groups) {
      const groupChatFile = join(GROUP_CHATS_DIR, `${group}.json`);
      
      if (!fs.existsSync(groupChatFile)) {
        // 创建新的空聊天记录文件
        fs.writeFileSync(groupChatFile, '[]', 'utf8');
        results[group] = '已创建';
      } else {
        results[group] = '已存在';
      }
    }
    
    res.json({ success: true, results });
  } catch (error) {
    console.error('初始化群组聊天错误:', error);
    res.status(500).json({ error: '初始化群组聊天记录失败' });
  }
});

// [辅助后端步骤 2.3] 聊天记录获取 API
app.get('/chat/groups/:groupName/log', (req, res) => {
  try {
    const { groupName } = req.params;
    const groupChatFile = join(GROUP_CHATS_DIR, `${groupName}.json`);
    
    if (!fs.existsSync(groupChatFile)) {
      // 如果群组聊天记录文件不存在，自动创建空文件
      fs.writeFileSync(groupChatFile, '[]', 'utf8');
      console.log(`为群组 ${groupName} 创建了新的聊天记录文件`);
      return res.json([]);
    }
    
    // 读取并返回该群组的聊天记录
    const data = fs.readFileSync(groupChatFile, 'utf8');
    const chatLog = JSON.parse(data);
    
    res.json(chatLog);
  } catch (error) {
    console.error('获取群组聊天记录错误:', error);
    res.status(500).json({ error: '获取群组聊天记录失败' });
  }
});

// [辅助后端步骤 2.4] 消息追加 API
app.post('/chat/groups/append-message', (req, res) => {
  try {
    const { groupName, username, message } = req.body;
    
    if (!groupName || !username || !message) {
      return res.status(400).json({ error: '群组名、用户名和消息内容不能为空' });
    }
    
    const groupChatFile = join(GROUP_CHATS_DIR, `${groupName}.json`);
    
    if (!fs.existsSync(groupChatFile)) {
      // 如果聊天记录文件不存在，创建一个新的
      fs.writeFileSync(groupChatFile, '[]', 'utf8');
    }
    
    // 读取现有聊天记录
    const data = fs.readFileSync(groupChatFile, 'utf8');
    const chatLog = JSON.parse(data);
    
    // 创建新消息对象
    const newMessage = {
      id: Date.now().toString(), // 使用时间戳作为消息ID
      username,
      message,
      timestamp: new Date().toISOString()
    };
    
    // 添加新消息到日志
    chatLog.push(newMessage);
    
    // 保存更新后的聊天记录
    fs.writeFileSync(groupChatFile, JSON.stringify(chatLog, null, 2), 'utf8');
    
    res.json({ success: true, message: '消息已添加', newMessage });
  } catch (error) {
    console.error('添加聊天消息错误:', error);
    res.status(500).json({ error: '添加聊天消息失败' });
  }
});

// 提供静态文件服务（如果需要）
app.use(express.static('public'));

// 根路由 - 显示欢迎信息和API文档
app.get('/', (req, res) => {
  // 获取在线用户数量
  const onlineUsers = JSON.parse(fs.readFileSync(ONLINE_USERS_FILE, 'utf8'));
  
  // 获取群组列表
  const groupFiles = fs.readdirSync(GROUP_CHATS_DIR)
    .filter(file => file.endsWith('.json'))
    .map(file => file.replace('.json', ''));
  
  res.send(`
    <html>
      <head>
        <title>Teedy群组聊天系统 - 聊天服务</title>
        <style>
          body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 20px;
            color: #333;
          }
          h1 {
            color: #2c3e50;
            border-bottom: 1px solid #eee;
            padding-bottom: 10px;
          }
          h2 {
            color: #3498db;
            margin-top: 20px;
          }
          .endpoint {
            background-color: #f8f9fa;
            border-left: 4px solid #3498db;
            padding: 10px;
            margin-bottom: 10px;
          }
          code {
            background-color: #f1f1f1;
            padding: 2px 5px;
            border-radius: 3px;
            font-family: monospace;
          }
          .stats {
            background-color: #e9f7fe;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
          }
        </style>
      </head>
      <body>
        <h1>Teedy群组聊天系统 - 聊天服务</h1>
        
        <div class="stats">
          <p>在线用户数: <strong>${onlineUsers.length}</strong></p>
          <p>群组数量: <strong>${groupFiles.length}</strong></p>
          <p>数据目录: <code>${DATA_DIR}</code></p>
        </div>
        
        <h2>API端点</h2>
        <div class="endpoint">
          <h3>用户状态</h3>
          <p><strong>POST</strong> <code>/notify/login</code> - 用户登录通知</p>
          <p><strong>POST</strong> <code>/notify/logout</code> - 用户登出通知</p>
          <p><strong>GET</strong> <code>/chat/online-users</code> - 获取在线用户列表</p>
        </div>
        
        <div class="endpoint">
          <h3>群组聊天</h3>
          <p><strong>POST</strong> <code>/chat/groups/init</code> - 初始化群组聊天</p>
          <p><strong>GET</strong> <code>/chat/groups/:groupName/log</code> - 获取群组聊天记录</p>
          <p><strong>POST</strong> <code>/chat/groups/append-message</code> - 添加新聊天消息</p>
        </div>
        
        <h2>当前群组</h2>
        <ul>
          ${groupFiles.map(group => `<li>${group}</li>`).join('')}
        </ul>
        
        <p>服务器状态: <strong>运行中</strong></p>
      </body>
    </html>
  `);
});

// 启动服务器
const server = createServer(app);

server.listen(PORT, () => {
  console.log(`聊天服务器运行中: http://localhost:${PORT}`);
  console.log(`数据目录: ${DATA_DIR}`);
  console.log(`群组聊天目录: ${GROUP_CHATS_DIR}`);
}); 