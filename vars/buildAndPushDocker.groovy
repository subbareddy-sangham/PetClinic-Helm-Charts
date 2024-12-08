def call(Map params = [:]) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('shortCommitSha') : "Missing required parameter: shortCommitSha"

    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String shortCommitSha = params.shortCommitSha
    String buildNumber = env.BUILD_NUMBER ?: '0'

    // Updated ECR image tag with full path
    String imageTag = "${shortCommitSha}-build-${buildNumber}"
    String ecrImageTag = "${repoUrl}/${dockerImageName}:${imageTag}"

    // Build Docker Image
    sh "docker build -t ${ecrImageTag} ."

    // Push Docker Image to ECR
    withCredentials([[ 
        $class: 'AmazonWebServicesCredentialsBinding', 
        credentialsId: awsCredentialsId 
    ]]) {
        sh """
        aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin ${repoUrl}
        docker push ${ecrImageTag}
        """
    }

    echo "Successfully built and pushed Docker image: ${ecrImageTag}"
}
