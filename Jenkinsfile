pipeline {
    agent any
    environment {
        // 定义环境变量
        // Jenkins 凭据配置，Docker Hub 凭据 ID 存储在 Jenkins 中
        DOCKER_HUB_CREDENTIALS = credentials('2')
        // Docker Hub 仓库名称
        DOCKER_IMAGE = 'viayu/teedy'
        // 使用构建号作为标签
        DOCKER_TAG ="${env.BUILD_NUMBER}"
    }
    stages {
        stage("构建") {
            steps {
                // 检出代码
                checkout scmGit(
                    branches: [[name:"558419a"]],
                    extensions:[],
                    userRemoteConfigs: [[url:'https://github.com/viayuu/Teedy.git']]
                )
                // 跳过测试，清理并打包
                sh 'mvn -B -DskipTests clean package'
            }
        }
        // 构建 Docker 镜像
        stage('构建镜像') {
            steps {
                script {
                    // 假设 Dockerfile 位于根目录
                    docker.build("$env.DOCKER_IMAGE:${env.DOCKER_TAG}")
                }
            }
        }
        // 将 Docker 镜像上传到 Docker Hub
        stage('上传镜像') {
            steps {
                script {
                    // 登录 Docker Hub
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKER_HUB_CREDENTIALS') {
                        // 推送镜像
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        // 可选：标记为 latest
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push("latest")
                    }
                }
            }
        }
        // 运行 Docker 容器
        stage('运行容器') {
            steps {
                script {
                    // 停止并删除已存在的容器（如果有）
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    // 运行容器
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8081 -d -p 8081:8080'
                    )
                    // 可选：列出所有 teedy 容器
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
} 