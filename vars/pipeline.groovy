@Library('Jenkins_shared_library') _  #name used in jenkins system for library
def COLOR_MAP = [
    'FAILURE' : 'danger',
    'SUCCESS' : 'good'
]

pipeline{
    agent any
    parameters {
        choice(name: 'action', choices: 'create\ndelete', description: 'Select create or destroy.')
        string(name: 'DOCKER_HUB_USERNAME', defaultValue: 'fir3eye', description: 'Docker Hub Username')
        string(name: 'IMAGE_NAME', defaultValue: 'youtube', description: 'Docker Image Name')
    }
    tools{
        jdk 'jdk17'
        nodejs 'node16'
    }
    environment {
        SCANNER_HOME=tool 'sonar-scanner'
    }
    stages{
        stage('clean workspace'){
            steps{
                01_cleanWorkspace()
            }
        }
        stage('checkout from Git'){
            steps{
                02_checkoutGit('https://github.com/Aj7Ay/Youtube-clone-app.git', 'main')
            }
        }
        stage('sonarqube Analysis'){
        when { expression { params.action == 'create'}}    
            steps{
                03_sonarAnalysis()
            }
        }
        stage('sonarqube QualitGate'){
        when { expression { params.action == 'create'}}    
            steps{
                script{
                    def credentialsId = 'Sonar-token'
                    04_qualityGate(credentialsId)
                }
            }
        }
        stage('Npm'){
        when { expression { params.action == 'create'}}    
            steps{
                05_npmInstall()
            }
        }
        stage('OWASP FS SCAN') {
            steps {
                dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('Trivyfile Scan') {
            steps{
                06_trivyFilescan()
            }
        }
        stage('Docker Build'){
        when { expression { params.action == 'create'}}    
            steps{
                script{
                   def dockerHubUsername = params.DOCKER_HUB_USERNAME
                   def imageName = params.IMAGE_NAME

                   07_dockerBuild(dockerHubUsername, imageName)
                }
            }
        }
        stage('Trivy iamge'){
        when { expression { params.action == 'create'}}    
            steps{
                08_trivyImage()
            }
        }
        stage('Run container'){
        when { expression { params.action == 'create'}}    
            steps{
                09_runContainer()
            }
        }
        stage('Remove container'){
        when { expression { params.action == 'delete'}}    
            steps{
                10_removeContainer()
            }
        }
        stage('Kube deploy'){
        when { expression { params.action == 'create'}}    
            steps{
                11_kubeDeploy()
            }
        }
        stage('kube deleter'){
        when { expression { params.action == 'delete'}}    
            steps{
                12_kubeDelete()
            }
        }


     }
     post {
         always {
             echo 'Slack Notifications'
             slackSend (
                 channel: '#Youtube-clone',   #change your channel name
                 color: COLOR_MAP[currentBuild.currentResult],
                 message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} \n build ${env.BUILD_NUMBER} \n More info at: ${env.BUILD_URL}"
               )
           }
       }
   }
