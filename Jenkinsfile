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
                // 即使 javadoc 失败也继续
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'mvn javadoc:javadoc'
                }
            }
        }
        stage('Site') {
            steps {
                // 即使 site 生成失败也继续
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'mvn site'
                }
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
