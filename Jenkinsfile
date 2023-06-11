pipeline {
    agent any
    tools {
        maven 'MAVEN-Jenkins'
        jdk 'OpenJDK 11'
    }
    environment {

            MY_ARTEFACT="integroBridgeService"
            MY_REPO="AdobeCampaignQE"
            MY_PATH="${env.MY_REPO}/${env.MY_ARTEFACT}"

            GIT_API_URL = "https://git.corp.adobe.com/api/v3/repos/${env.MY_PATH}/pulls"
            GIT_API_CREDENTIALS = 'GIT_API_PWD'

            gitScriptedURL = ''

            snapshotVersion = ''
        }


    stages {

        stage('Run Tests') {
            steps {

                    echo "Updating PR with the deployment Package."

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'ARTIFACTORY_API_TOKEN',
                        usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_API_TOKEN']]) {
                            
                                sh """
                                mvn clean install \
                            """
                    }



            }
        }
    }
    post {
            always {
                    jacoco(
                            execPattern: 'integroBridgeService/target/**.exec',
                            sourcePattern: 'integroBridgeService/src/main/java',
                            sourceInclusionPattern: 'integroBridgeService/**/*.java',
                            changeBuildStatus: true,
                            buildOverBuild: true,
                            minimumBranchCoverage: '85',
                            deltaBranchCoverage: '0.3',
                            minimumClassCoverage: '95',
                            deltaClassCoverage: '1',
                            minimumMethodCoverage: '90',
                            deltaMethodCoverage: '1',
                            minimumComplexityCoverage: '70',
                            deltaComplexityCoverage: '1',
                            minimumLineCoverage: '87',
                            deltaLineCoverage: '0.1',
                            deltaInstructionCoverage: '0.3'
                        )

               step([$class: 'Publisher', reportFilenamePattern: 'integroBridgeService/target/surefire-reports/testng-results.xml'])

               archiveArtifacts allowEmptyArchive: true, artifacts: "**"
            }

            cleanup {
               cleanWs()
            }


    }
}
