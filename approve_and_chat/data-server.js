import express from 'express';
import cors from 'cors';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const PORT = 3001;
const DATA_FILE = join(__dirname, 'data', 'registrations.json');
const GROUPS_FILE = join(__dirname, 'data', 'groups.json');

// 确保data目录存在
if (!fs.existsSync(join(__dirname, 'data'))) {
  fs.mkdirSync(join(__dirname, 'data'));
}

// 如果数据文件不存在，创建一个空的JSON数组
if (!fs.existsSync(DATA_FILE)) {
  fs.writeFileSync(DATA_FILE, '[]', 'utf8');
}

// 如果群组数据文件不存在，创建一个空的JSON数组
if (!fs.existsSync(GROUPS_FILE)) {
  fs.writeFileSync(GROUPS_FILE, '[]', 'utf8');
}

// 中间件
app.use(cors()); // 允许跨域请求
app.use(express.json()); // 解析JSON请求体

// 获取所有注册请求
app.get('/api/registrations', (req, res) => {
  try {
    const data = fs.readFileSync(DATA_FILE, 'utf8');
    res.json(JSON.parse(data));
  } catch (error) {
    console.error('Error reading registrations:', error);
    res.status(500).json({ error: 'Failed to read registrations' });
  }
});

// 添加新的注册请求
app.post('/api/registrations', (req, res) => {
  try {
    const newRegistration = req.body;
    
    // 读取现有数据
    const data = fs.readFileSync(DATA_FILE, 'utf8');
    const registrations = JSON.parse(data);
    
    // 检查是否已存在同名用户的申请
    const existingIndex = registrations.findIndex(reg => reg.username === newRegistration.username);
    
    if (existingIndex >= 0) {
      // 更新已存在的申请
      registrations[existingIndex] = newRegistration;
    } else {
      // 添加新申请
      registrations.push(newRegistration);
    }
    
    // 保存回文件
    fs.writeFileSync(DATA_FILE, JSON.stringify(registrations, null, 2), 'utf8');
    
    res.status(201).json({ success: true, message: 'Registration saved' });
  } catch (error) {
    console.error('Error saving registration:', error);
    res.status(500).json({ error: 'Failed to save registration' });
  }
});

// 删除注册请求
app.delete('/api/registrations/:username', (req, res) => {
  try {
    const username = req.params.username;
    
    // 读取现有数据
    const data = fs.readFileSync(DATA_FILE, 'utf8');
    let registrations = JSON.parse(data);
    
    // 过滤掉要删除的申请
    registrations = registrations.filter(reg => reg.username !== username);
    
    // 保存回文件
    fs.writeFileSync(DATA_FILE, JSON.stringify(registrations, null, 2), 'utf8');
    
    res.json({ success: true, message: 'Registration deleted' });
  } catch (error) {
    console.error('Error deleting registration:', error);
    res.status(500).json({ error: 'Failed to delete registration' });
  }
});

// 获取所有群组
app.get('/api/groups', (req, res) => {
  try {
    if (!fs.existsSync(GROUPS_FILE)) {
      return res.json([]);
    }
    
    const data = fs.readFileSync(GROUPS_FILE, 'utf8');
    res.json(JSON.parse(data));
  } catch (error) {
    console.error('Error reading groups:', error);
    res.status(500).json({ error: 'Failed to read groups' });
  }
});

// 保存群组数据
app.post('/api/groups', (req, res) => {
  try {
    const groups = req.body;
    
    // 确保是数组
    if (!Array.isArray(groups)) {
      return res.status(400).json({ error: '群组数据必须是数组格式' });
    }
    
    // 保存到文件
    fs.writeFileSync(GROUPS_FILE, JSON.stringify(groups, null, 2), 'utf8');
    
    res.status(201).json({ success: true, message: '群组数据已保存', count: groups.length });
  } catch (error) {
    console.error('Error saving groups:', error);
    res.status(500).json({ error: 'Failed to save groups' });
  }
});

// 提供静态文件服务
app.use(express.static('public'));
app.use(express.static('dist'));

// 根路由 - 显示欢迎信息和API文档
app.get('/', (req, res) => {
  res.send(`
    <html>
      <head>
        <title>Teedy用户注册申请系统 - 数据服务</title>
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
        </style>
      </head>
      <body>
        <h1>Teedy用户注册申请系统 - 数据服务</h1>
        <p>数据文件位置: <code>${DATA_FILE}</code></p>
        <p>当前存储的注册申请数量: <strong>${JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')).length}</strong></p>
        
        <h2>API端点</h2>
        <div class="endpoint">
          <p><strong>GET</strong> <code>/api/registrations</code> - 获取所有注册申请</p>
          <p><strong>POST</strong> <code>/api/registrations</code> - 添加新的注册申请</p>
          <p><strong>DELETE</strong> <code>/api/registrations/:username</code> - 删除指定的注册申请</p>
        </div>
        
        <p>服务器状态: <strong>运行中</strong></p>
      </body>
    </html>
  `);
});

app.listen(PORT, () => {
  console.log(`Data server running at http://localhost:${PORT}`);
  console.log(`Data file: ${DATA_FILE}`);
  console.log(`Groups file: ${GROUPS_FILE}`);
}); 