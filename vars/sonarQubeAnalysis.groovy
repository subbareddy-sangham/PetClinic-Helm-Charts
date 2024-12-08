def call(Map params = [:]) {
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    String namespace = params.namespace
    String sonarUrl
    String sonarProjectKey
    String sonarToken

    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.awsCredentialsId]]) {
        // Validate namespace
        try {
            sh "kubectl get namespace ${namespace} >/dev/null 2>&1 || { echo 'Namespace ${namespace} not found'; exit 1; }"
        } catch (Exception e) {
            error "Namespace validation failed: ${e.message}. Ensure the namespace '${namespace}' exists and is accessible."
        }

        // Retrieve SonarQube configurations
        try {
            sonarUrl = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_URL}' || echo ''", returnStdout: true).trim()
            sonarProjectKey = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_PROJECT_KEY}' || echo ''", returnStdout: true).trim()
            sonarToken = sh(script: "kubectl get secret sonar-secrets -n ${namespace} -o=jsonpath='{.data.SONAR_TOKEN}' | base64 --decode || echo ''", returnStdout: true).trim()
        } catch (Exception e) {
            error "Failed to retrieve SonarQube configurations: ${e.message}"
        }

        // Validate retrieved configurations
        if (!sonarUrl) {
            error "SONAR_URL is missing in ConfigMap 'sonar-config' in namespace ${namespace}"
        }
        if (!sonarProjectKey) {
            error "SONAR_PROJECT_KEY is missing in ConfigMap 'sonar-config' in namespace ${namespace}"
        }
        if (!sonarToken) {
            error "SONAR_TOKEN is missing in Secret 'sonar-secrets' in namespace ${namespace}"
        }

        // Log non-sensitive details
        echo "SonarQube URL retrieved successfully"
        echo "SonarQube Project Key: ${sonarProjectKey}"

        // Debugging: List workspace contents (optional)
        if (params.debug) {
            echo "Listing workspace contents for debugging:"
            sh "ls -la"
        }

        // Execute SonarQube analysis
        try {
            withEnv([
                "SONAR_HOST_URL=${sonarUrl}",
                "SONAR_PROJECT_KEY=${sonarProjectKey}",
                "SONAR_TOKEN=${sonarToken}"
            ]) {
                sh "./mvnw sonar:sonar -Dsonar.host.url=${sonarUrl} -Dsonar.projectKey=${sonarProjectKey} -Dsonar.login=${sonarToken}"
            }
        } catch (Exception e) {
            error "SonarQube analysis failed: ${e.message}"
        }
    }
}
