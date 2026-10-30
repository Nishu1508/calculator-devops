// =============================================================================
// Declarative CI/CD pipeline for the Calculator REST API
//   Checkout -> Build -> Test -> Docker Build -> Push -> Deploy
//
// Triggered automatically by a GitHub push (webhook), with Poll SCM as a fallback.
// Needs on the Jenkins side (see jenkins/casc.yaml):
//   - credential  'dockerhub-credentials'  (username + access token)
//   - global env  DOCKERHUB_USER, NOTIFY_EMAIL
//   - Maven, Docker CLI and Docker Compose on the agent
// =============================================================================

pipeline {
    agent any

    triggers {
        githubPush()              // GitHub webhook -> http://<jenkins>/github-webhook/
        pollSCM('H/2 * * * *')    // fallback: check GitHub every ~2 minutes for new commits
    }

    options {
        skipDefaultCheckout()     // we check out explicitly in the 'Checkout' stage
        timestamps()
        disableConcurrentBuilds() // never run two deployments at the same time
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        IMAGE_NAME    = "${env.DOCKERHUB_USER}/calculator-api"
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        // Per-build Docker client config, so the Docker Hub login is never left in JENKINS_HOME.
        DOCKER_CONFIG = "${env.WORKSPACE}@tmp/docker-config"
        // Reproducible image IDs: without default provenance attestations an unchanged (cached)
        // rebuild yields the same image ID, so redeploys do not needlessly re-tag the Nginx image.
        BUILDX_NO_DEFAULT_ATTESTATIONS = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_COMMIT_MSG   = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
                    env.GIT_AUTHOR       = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                }
                echo "Building commit ${env.GIT_COMMIT_SHORT} by ${env.GIT_AUTHOR}: ${env.GIT_COMMIT_MSG}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/calculator.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                // A failing test makes mvn exit non-zero, which fails this stage and the build.
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    script {
                        if (fileExists('target/site/jacoco/jacoco.xml')) {
                            recordCoverage(tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                                           id: 'jacoco', name: 'JaCoCo Coverage')
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      --label org.opencontainers.image.revision="$GIT_COMMIT_SHORT" \
                      --label org.opencontainers.image.version="$IMAGE_TAG" \
                      -t "$IMAGE_NAME:$IMAGE_TAG" \
                      -t "$IMAGE_NAME:latest" \
                      .
                    docker image ls "$IMAGE_NAME"
                '''
            }
        }

        stage('Push') {
            steps {
                // Credentials come from the Jenkins Credentials store; Jenkins masks them in the log.
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials',
                                                  usernameVariable: 'DOCKERHUB_LOGIN',
                                                  passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_LOGIN" --password-stdin
                        docker push "$IMAGE_NAME:$IMAGE_TAG"
                        docker push "$IMAGE_NAME:latest"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // docker-compose.yml reads DOCKERHUB_USER and IMAGE_TAG from the environment.
                // Re-running with the same tag changes nothing; a new tag recreates only the API
                // containers. The fixed project name ("calculator") keeps it one stack -> idempotent.
                sh '''
                    docker compose config --quiet
                    docker compose build nginx
                    docker compose up -d --no-build --remove-orphans --wait --wait-timeout 180
                    docker compose ps
                '''
                // Verify health and do a smoke test through the Nginx proxy.
                sh '''
                    health=$(docker compose exec -T nginx wget -qO- http://127.0.0.1:8080/actuator/health)
                    echo "Health: $health"
                    echo "$health" | grep -q '"status":"UP"'

                    result=$(docker compose exec -T nginx wget -qO- "http://127.0.0.1:8080/api/v1/add?a=2&b=3")
                    echo "Smoke test /api/v1/add?a=2&b=3 -> $result"
                    echo "$result" | grep -q '"result":5'
                '''
            }
        }
    }

    post {
        success {
            notify('SUCCESS')
        }
        failure {
            notify('FAILURE')
        }
        unstable {
            notify('UNSTABLE')
        }
        always {
            // Remove dangling layers left by rebuilt calculator images (other images are untouched).
            sh 'docker image prune -f --filter "label=org.opencontainers.image.title=calculator-api" || true'
        }
    }
}

// Sends the build-status email (SMTP settings come from jenkins/casc.yaml).
def notify(String status) {
    def colour = status == 'SUCCESS' ? '#2e7d32' : (status == 'UNSTABLE' ? '#f9a825' : '#c62828')
    emailext(
        to: env.NOTIFY_EMAIL,
        subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER}: ${status}",
        mimeType: 'text/html',
        attachLog: status != 'SUCCESS',
        compressLog: true,
        body: """
            <h2 style="color:${colour}">Build ${status}</h2>
            <table cellpadding="4">
              <tr><td><b>Job</b></td><td>${env.JOB_NAME}</td></tr>
              <tr><td><b>Build</b></td><td>#${env.BUILD_NUMBER} (${currentBuild.durationString.replace(' and counting', '')})</td></tr>
              <tr><td><b>Commit</b></td><td>${env.GIT_COMMIT_SHORT ?: 'n/a'} - ${env.GIT_COMMIT_MSG ?: ''} (${env.GIT_AUTHOR ?: ''})</td></tr>
              <tr><td><b>Image</b></td><td>${env.IMAGE_NAME}:${env.IMAGE_TAG}</td></tr>
              <tr><td><b>App URL</b></td><td><a href="http://localhost:8081/actuator/health">http://localhost:8081</a></td></tr>
            </table>
            <p><a href="${env.BUILD_URL}">Open build in Jenkins</a> | <a href="${env.BUILD_URL}console">Console output</a></p>
        """
    )
}
