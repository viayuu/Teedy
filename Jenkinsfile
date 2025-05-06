pipeline {
    agent any

    stages {
        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Test') {
            steps {
                // 单独通过 Maven 参数忽略测试失败
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
        }
        stage('PMD') {
            steps {
                sh 'mvn pmd:pmd'
            }
        }
        stage('JaCoCo') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
        stage('Javadoc') {
            steps {
                script {
                    try {
                        // 运行Javadoc命令，捕获可能错误
                        sh 'mvn javadoc:javadoc'
                    } catch (Exception e) {
                        echo "Javadoc generation failed with error: ${e}. Ignoring and continuing build..."
                        // 设置构建状态为UNSTABLE，以标记问题但不失败
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        stage('Site') {
            steps {
                // 运行Site命令，确保即使Javadoc失败，Site也能尝试生成
                sh 'mvn site'
            }
        }
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true
            archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
            junit '**/target/surefire-reports/*.xml'
        }
    }
}
