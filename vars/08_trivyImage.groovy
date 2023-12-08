def call() {
    sh 'trivy image fir3eye/youtube:latest > trivyimage.txt'
}
