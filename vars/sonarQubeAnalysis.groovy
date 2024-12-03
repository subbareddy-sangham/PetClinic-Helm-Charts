def call(Map params = [:]) {
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    String namespace = params.namespace
    String sonarUrl
    String sonarProjectKey
    String sonarToken

    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.awsCredentialsId]]) {
        // Validate namespace
        sh "kubectl get namespace ${namespace} || { echo 'Namespace not found'; exit 1; }"

        // Retrieve SonarQube configurations
        sonarUrl = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_URL}' || echo ''", returnStdout: true).trim()
        sonarProjectKey = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_PROJECT_KEY}' || echo ''", returnStdout: true).trim()
        sonarToken = sh(script: "kubectl get secret sonar-secrets -n ${namespace} -o=jsonpath='{.data.SONAR_TOKEN}' | base64 --decode || echo ''", returnStdout: true).trim()

        if (!sonarUrl || !sonarProjectKey || !sonarToken) {
            error "Failed to retrieve SonarQube configurations from Kubernetes resources in namespace ${namespace}"
        }

        echo "SonarQube URL: ${sonarUrl}"
        echo "Project Key: ${sonarProjectKey}"

        // Debugging step: List workspace contents
        sh "ls -la"

        // Execute SonarQube analysis
        withEnv([
            "SONAR_HOST_URL=${sonarUrl}", 
            "SONAR_PROJECT_KEY=${sonarProjectKey}", 
            "SONAR_TOKEN=${sonarToken}"
        ]) {
            sh "./mvnw sonar:sonar -Dsonar.host.url=${sonarUrl} -Dsonar.projectKey=${sonarProjectKey} -Dsonar.login=${sonarToken}"
        }
    }
}
