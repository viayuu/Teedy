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
                    branches: [[name:"practice12"]],
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
                    docker.withRegistry('https://registry.hub.docker.com', '2') {
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
                    // 停止并删除旧容器
                    sh "docker stop teedy-container-8082 || true && docker rm teedy-container-8082 || true"
                    sh "docker stop teedy-container-8083 || true && docker rm teedy-container-8083 || true"
                    sh "docker stop teedy-container-8084 || true && docker rm teedy-container-8084 || true"
                    sh "docker stop teedy-container-8085 || true && docker rm teedy-container-8085 || true"
                    sh "docker stop teedy-container-8086 || true && docker rm teedy-container-8086 || true"
                    sh "docker stop teedy-container-8087 || true && docker rm teedy-container-8087 || true"
                    sh "docker stop teedy-container-8089 || true && docker rm teedy-container-8089 || true"
                    // 运行三个容器（每个容器单独一行，用分号或换行分隔）
                    sh """
                    docker run -d -p 8089:8080 --name teedy-container-8089 ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                    docker run -d -p 8086:8080 --name teedy-container-8086 ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                    docker run -d -p 8087:8080 --name teedy-container-8087 ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                    """
                    
                    // 可选：列出容器
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
} 