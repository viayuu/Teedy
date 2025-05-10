# Teedy用户注册申请系统

这是一个基于Vue 3的独立应用，用于实现Teedy文档管理系统的用户注册申请功能。

## 功能特点

- 访客用户可以提交注册申请
- 管理员可以查看和处理注册申请
- 管理员可以直接批准或拒绝申请
- 批准后自动通过Teedy API创建新用户

## 技术栈

- Vue 3
- Bootstrap 5
- Axios
- Node.js + Express (用于存储申请数据)
- Vite

## 开发设置

### 安装依赖

```bash
npm install
```

### 启动数据服务器

```bash
npm run server
```

### 启动前端开发服务

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

## 使用方法

1. 确保Teedy系统已运行并可访问（默认为http://localhost:8080）
2. 启动数据服务器（保存注册请求数据）：`npm run server`
3. 启动前端应用：`npm run dev`
4. 使用访客(guest)账户登录Teedy系统
5. 访问前端应用，填写并提交注册申请
6. 切换到管理员账户，访问前端应用查看待处理的申请
7. 管理员可以批准或拒绝注册申请

## 注意事项

- 注册申请数据存储在服务器端的JSON文件中（data/registrations.json）
- 使用前必须先在Teedy系统中登录
- 管理员账户需要有创建用户的权限
- 前端应用和数据服务器需要同时运行才能正常使用 