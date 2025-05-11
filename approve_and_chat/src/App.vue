<template>
  <div class="container">
    <header class="my-4">
      <h1 class="text-center mb-4">Teedy增强功能平台</h1>
      
      <nav class="navbar navbar-expand-lg navbar-light bg-light mb-4">
        <div class="container-fluid">
          <a class="navbar-brand" href="#">Teedy扩展</a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
          </button>
          <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav">
              <li class="nav-item">
                <router-link to="/" class="nav-link" active-class="active">注册申请</router-link>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>
    
    <main>
      <router-view />
    </main>
    
    <footer class="mt-5 pt-4 text-center text-muted">
      <hr>
      <p>&copy; 2023 Teedy文档管理系统扩展功能</p>
    </footer>
  </div>
</template>

<style>
body {
  background-color: #f5f5f5;
}

.router-link-active {
  font-weight: bold;
}
</style> 