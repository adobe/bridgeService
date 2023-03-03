pipeline {
    agent any
    tools {
        maven 'MAVEN-Jenkins'
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
                                mvn clean test -U --settings .mvn/settings.xml \
                            """
                    }

                    jacoco( 
                        execPattern: 'target/**.exec', 
                        sourcePattern: 'src/main/java', 
                        sourceInclusionPattern: '**/*.java', 
                        changeBuildStatus: true, 
                        buildOverBuild: true, 
                        minimumBranchCoverage: '70', 
                        deltaBranchCoverage: '0.3', 
                        minimumClassCoverage: '95', 
                        deltaClassCoverage: '1', 
                        minimumMethodCoverage: '78',
                        deltaMethodCoverage: '1', 
                        minimumComplexityCoverage: '70', 
                        deltaComplexityCoverage: '1', 
                        minimumLineCoverage: '87', 
                        deltaLineCoverage: '0.1',
        			    deltaInstructionCoverage: '0.3'
                    )

                    step([$class: 'Publisher', reportFilenamePattern: 'target/surefire-reports/testng-results.xml'])

            }
        }

        /*
        stage('Comment Deployment') {
            steps {
                //Consider parallel jobs - multiple PR

                script {
                    
                    def String commentURL = "https://git.corp.adobe.com/api/v3/repos/${env.MY_PATH}/issues/${ghprbPullId}/comments"
                    
                    final String commentBody = """ 
                                    {
                                        "body": "New Deployment : ${snapshotVersion}"
                                    }   
                                """

                    httpRequest httpMode: 'POST', url: commentURL, authentication: 'GIT_API_PWD', requestBody: commentBody, responseHandle: 'NONE', wrapAsMultipart: false
                
                }
            }
        }
        */
        
    }
    post {
        cleanup {

        }
    }
}
