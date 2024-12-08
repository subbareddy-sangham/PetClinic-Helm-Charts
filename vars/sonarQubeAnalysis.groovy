def call(Map params = [:]) {
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    String namespace = params.namespace
    String sonarUrl = ""
    String sonarProjectKey = ""
    String sonarToken = ""

    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.awsCredentialsId]]) {
        // Retrieve SonarQube configurations directly (assumes Helm has created resources)
        try {
            sonarUrl = sh(script: "kubectl get configmap app-config-${namespace} -n ${namespace} -o=jsonpath='{.data.SONARQUBE_URL}' || echo ''", returnStdout: true).trim()
            sonarProjectKey = sh(script: "kubectl get configmap app-config-${namespace} -n ${namespace} -o=jsonpath='{.data.SONARQUBE_PROJECT_KEY}' || echo ''", returnStdout: true).trim()
            sonarToken = sh(script: "kubectl get secret ${namespace}-secrets -n ${namespace} -o=jsonpath='{.data.SONARQUBE_TOKEN}' | base64 --decode || echo ''", returnStdout: true).trim()
        } catch (Exception e) {
            error "Failed to retrieve SonarQube configurations: ${e.message}"
        }

        // Validate retrieved configurations
        if (!sonarUrl) {
            error "SONARQUBE_URL is missing in ConfigMap 'app-config-${namespace}' in namespace ${namespace}"
        }
        if (!sonarProjectKey) {
            error "SONARQUBE_PROJECT_KEY is missing in ConfigMap 'app-config-${namespace}' in namespace ${namespace}"
        }
        if (!sonarToken) {
            error "SONARQUBE_TOKEN is missing in Secret '${namespace}-secrets' in namespace ${namespace}"
        }

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
