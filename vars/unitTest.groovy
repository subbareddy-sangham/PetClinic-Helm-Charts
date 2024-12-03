def call(Map params = [:]) {
    String testCommand = params.get('testCommand', './mvnw test')
    String stageName = params.get('stageName', 'Unit Testing')
    String reportDir = params.get('reportDir', 'target/surefire-reports')

    echo "Running tests for stage: ${stageName}"
    echo "Command: ${testCommand}"
    echo "Workspace: ${pwd()}"
    
    try {
        // List current workspace files for debugging
        sh 'ls -la'

        // Run the test command
        sh testCommand
        echo "Tests completed successfully."

        // Check for and publish test reports
        if (fileExists("${reportDir}/*.xml")) {
            junit "${reportDir}/*.xml"
        } else {
            echo "No test reports found in ${reportDir}"
        }
    } catch (Exception e) {
        error "Tests failed during ${stageName}: ${e.message}"
    }
}
