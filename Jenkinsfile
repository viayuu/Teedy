pipeline {
    agent any
environment {
    DEPLOYMENT_NAME = "hello-node"      // Deployment 名称（正确）
    CONTAINER_NAME = "teedy"            // 容器名称（从 Deployment 中确认）
    IMAGE_NAME = "viayu/teedy:12"  // 镜像名称（格式：<仓库>/<镜像名>:<标签>）
}
    stages {
        stage('Start Minikube') {
            steps {
                sh '''
                if ! minikube status | grep -q "Running"; then
                    echo "Starting Minikube..."
                    minikube start
                else
                    echo "Minikube already running."
                fi
                '''
            }
        }
        stage('Set Image') {
            steps {
                sh "kubectl set image deployment/${DEPLOYMENT_NAME} ${CONTAINER_NAME}=${IMAGE_NAME}"
            }
        }
        stage('Verify') {
            steps {
                sh "kubectl rollout status deployment/${DEPLOYMENT_NAME}"  // 验证部署状态
                sh "kubectl get pods"  // 检查 Pod 是否更新为新镜像
            }
        }
    }
}
