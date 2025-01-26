pipeline {
    agent any
    stages {
        stage('checkout') {
            steps {
                echo "Checkout the lenneflow project!"
                //git branch: 'main', url: 'https://github.com/lenneflow/lenneflow-k8s-api.git'
                git credentialsId: 'f5f2b9de-3117-4386-a3a5-7e67d6d9f91b', url: 'https://github.com/lenneflow/lenneflow-k8s-api.git'
            }
        }
        stage('Build And Test') {
            steps {
                script{
                    echo 'Start building...'
                    sh 'chmod +x gradlew'
                    sh './gradlew test'
                }
            }
        }
        stage('Create test Report') {
            steps {
                script{
                    echo "Start allure report"
                    allure includeProperties:
                    false,
                    jdk: '',
                    results: [[path: 'build/test-results/test']]
                }
            }
        }
    
    }
    post{
        failure {
                echo "Handle failures"
                emailext body: '$DEFAULT_CONTENT', subject: 'JENKINS NOTIFICATION - $DEFAULT_SUBJECT', to: 'dev@ganemtore.net'
            } 
    }
}