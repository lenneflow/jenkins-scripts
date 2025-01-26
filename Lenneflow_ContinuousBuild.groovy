pipeline {
    agent any
    stages {
        stage('checkout') {
            steps {
                echo "Checkout the lenneflow project!"
                git credentialsId: 'f5f2b9de-3117-4386-a3a5-7e67d6d9f91b', branch: 'master', url: 'https://github.com/lenneflow/lenneflow.git'
            }
        }
        stage('Build And Test') {
            steps {
                script{
                    try {
                        sh 'chmod +x gradlew'
                        sh './gradlew AllUnitTests'
                    } catch (err) {
                        echo err.getMessage()
                    }
                }
            }
        }
        stage('Create test Report') {
            steps {
                script{
                sh 'rm -rf test-results'
                sh 'mkdir -p test-results'
                List<String> services = [ 'worker-service', 'function-service','workflow-service','callback-service','orchestration-service', 'account-service'] as String[]
                for(service in services){
                        echo "Collect test results of $service"
                        sh "cp $service/build/test-results/**/*.xml test-results"
                    }
                }
            }
            post{
                always {
                    allure includeProperties:
                    false,
                    jdk: '',
                    results: [[path: 'test-results']]
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