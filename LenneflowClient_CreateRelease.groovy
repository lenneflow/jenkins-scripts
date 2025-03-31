def RELEASE_NAME_PREFIX = "lenneflow-client-"
def NEW_VERSION = ""
def VERSION_COMMIT_PREFIX = "Added new Lenneflow client Version:"
pipeline {
    agent any
    stages {
        stage('checkout') {
            steps {
                echo "Checkout the project!"
                deleteDir() 
                checkout changelog: true, scm: scmGit(branches: [[name: 'origin/master']], extensions: [[$class: 'PathRestriction', excludedRegions: '', includedRegions: '']], userRemoteConfigs: [[credentialsId: 'f5f2b9de-3117-4386-a3a5-7e67d6d9f91b', url: 'https://github.com/lenneflow/lenneflow-client.git']])
                echo 'Set GIT global variables'
                
            }
        }
        stage('Increment Version') {
            steps {
                script{
                    echo "read the current version"
                    def props = readProperties  file: 'gradle.properties'
                    echo "$props"
                    def currentVersion = props["version"]
                    echo "the current version is $currentVersion"
                    
                    def splitted = currentVersion.split("\\.")
                    int patch = splitted[2].trim() as Integer
                    int newPatch = patch + 1
                    NEW_VERSION = splitted[0] + "." + splitted[1] + "." + "$newPatch"
                    echo "the new version is $NEW_VERSION"
                    writeFile(file: "gradle.properties", text: "version=$NEW_VERSION", encoding: "UTF-8")

                }
            }
        }
        stage('Build') {
            steps {
                script{
                    echo 'Start building...'
                    sh 'chmod +x gradlew'
                    sh './gradlew bootJar'
                }
            }
        }
        stage('Deploy') {
            steps {
                script{
                    def releaseName = "$RELEASE_NAME_PREFIX" + "$NEW_VERSION" + ".jar"
                    echo "Start deploying the release $releaseName"
                    
                    writeFile file: 'test.md', text: 'This is a test message.'
                    createGitHubRelease(
                        credentialId: '285c254c-6320-4c8e-b6ee-db37f5229f6c',
                        repository: 'lenneflow/lenneflow-client',
                        commitish: 'master',
                        tag: "v$NEW_VERSION",
                        bodyFile: 'test.md'
                    )
                    uploadGithubReleaseAsset(
                        credentialId: '285c254c-6320-4c8e-b6ee-db37f5229f6c',
                        repository: 'lenneflow/lenneflow-client',
                        tagName: "v$NEW_VERSION", 
                        uploadAssets: [
                                [filePath: "${WORKSPACE}/build/libs/$releaseName"]
                        ]
                    )

                }
            }
        }
        stage('Commit Changes') {
            steps {
                script{
                    def commitMessage = "$VERSION_COMMIT_PREFIX"+ " " + "$NEW_VERSION"
                    withCredentials([gitUsernamePassword(credentialsId: 'f5f2b9de-3117-4386-a3a5-7e67d6d9f91b', gitToolName: 'Default')]) {
                        sh "git config --global user.email \"dev@ganemtore.net\""
                        sh "git config --global user.name \"Jenkins\""
                        sh "git config push.default current"
                        sh "git commit -am \"$commitMessage\""
                        sh "git push origin HEAD:master"
                    }
                }
            }
        }
    }
    post{
        failure {
                echo "Handle failures"
                //emailext body: '$DEFAULT_CONTENT', subject: 'JENKINS NOTIFICATION - $DEFAULT_SUBJECT', to: 'dev@ganemtore.net'
            } 
    }
}