pipeline {
    agent {
        kubernetes {
            label 'deploy-service-pod'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    job: deploy-service
spec:
  containers:
  - name: git
    image: alpine/git
    command: ["cat"]
    tty: true
  - name: helm-cli
    image: ibmcom/k8s-helm:v2.6.0
    command: ["cat"]
    tty: true
"""
        }
    }

    stages {

        stage('Find deployment descriptor') {
            steps {
                container('git') {
                        script {
                          
                            sh "git clone https://github.com/mynickwasbusy/habr-demo-app"
                            
                        }
                    }
                }
            }
        stage('Deploy to env') {
            steps {
                container('helm-cli') {
                    script {
                        dir ("${params.GIT_REPO}") {
                            sh "./helm/setRevision.sh ${params.VERSION}"
                            def registryIp = sh(script: 'getent hosts registry.kube-system | awk \'{ print $1 ; exit }\'', returnStdout: true).trim()
                            sh "/helm upgrade ${params.ENV}-${params.GIT_REPO.toLowerCase()} helm/ --install --namespace ${params.ENV} --set registry=${registryIp}"
                        }
                    }
                }
            }
        }
    }
