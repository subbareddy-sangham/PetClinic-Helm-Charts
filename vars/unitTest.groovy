def call(Map params = [:]) {
    String testCommand = params.get('testCommand', './mvnw test')
    String stageName = params.get('stageName', 'Unit Testing')
    String reportDir = params.get('reportDir', 'target/surefire-reports')

    echo "Running tests for stage: ${stageName}"
    echo "Command: ${testCommand}"

    try {
        sh testCommand
        echo "Tests completed successfully."

        // Publish test reports if they exist
        junit "${reportDir}/*.xml"
    } catch (Exception e) {
        error "Tests failed during ${stageName}: ${e.message}"
    }
}
