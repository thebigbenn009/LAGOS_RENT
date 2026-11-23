// =============================================================================
// LAGOS RENT PLATFORM — DECLARATIVE JENKINS PIPELINE
// Mirrors our GitHub Actions CI/CD workflow using the same portable scripts.
// =============================================================================

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    tools {
        jdk 'temurin-21'
        maven 'maven-3.9'
    }

    environment {
        REGISTRY       = 'ghcr.io'
        IMAGE_PREFIX   = 'ghcr.io/lagos-rent'
        // Securely binds username/password from Jenkins Credentials ID 'ghcr-credentials'
        REGISTRY_CREDS = credentials('ghcr-credentials')
    }

    stages {
        // =====================================================================
        // STAGE 1: Detect Changed Microservices in the Monorepo
        // =====================================================================
        stage('Detect Changed Services') {
            steps {
                script {
                    def baseRef = env.CHANGE_TARGET ? "origin/${env.CHANGE_TARGET}" : "HEAD~1"
                    def output = sh(
                        script: "./platform/scripts/detect-changed-services.sh ${baseRef}",
                        returnStdout: true
                    ).trim()
                    echo "Change detection result:\n${output}"

                    def mavenModules = output.split('\n')
                        .find { it.startsWith('maven_modules=') }
                        ?.replace('maven_modules=', '') ?: ''

                    env.CHANGED_MODULES = mavenModules
                }
            }
        }

        // =====================================================================
        // STAGE 2: Secret Scanning (Gitleaks Policy)
        // =====================================================================
        stage('Secret Scan (Gitleaks)') {
            steps {
                sh '''
                    docker run --rm -v "$WORKSPACE:/repo:ro" \
                      zricethezav/gitleaks:latest \
                      dir /repo --config /repo/.gitleaks.toml
                '''
            }
        }

        // =====================================================================
        // STAGE 3: Fast Unit & Contract Tests + Enforcer + JaCoCo
        // =====================================================================
        stage('Unit & Contract Tests') {
            when {
                expression { return env.CHANGED_MODULES != '' }
            }
            steps {
                sh 'mvn -B -pl "$CHANGED_MODULES" test'
            }
        }

        // =====================================================================
        // STAGE 4: Testcontainers Integration Tests & CycloneDX SBOM
        // =====================================================================
        stage('Integration Tests & SBOM') {
            when {
                expression { return env.CHANGED_MODULES != '' }
            }
            steps {
                sh 'mvn -B -pl "$CHANGED_MODULES" verify -Dsurefire.skip=true'
            }
        }

        // =====================================================================
        // STAGE 5: Build Layered Docker Images & Scan with Trivy
        // =====================================================================
        stage('Build Images & Trivy CVE Scan') {
            when {
                expression { return env.CHANGED_MODULES != '' }
            }
            steps {
                sh '''
                    GIT_SHA=$(git rev-parse --short HEAD)
                    IFS=',' read -ra MODULES <<< "$CHANGED_MODULES"
                    for mod in "${MODULES[@]}"; do
                      svc="${mod#services/}"
                      docker build -t "${IMAGE_PREFIX}/${svc}:sha-${GIT_SHA}" "$mod"
                      docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                        aquasec/trivy:latest image \
                        --severity CRITICAL --ignore-unfixed --exit-code 1 \
                        "${IMAGE_PREFIX}/${svc}:sha-${GIT_SHA}"
                    done
                '''
            }
        }

        // =====================================================================
        // STAGE 6: Push Immutable SHA Images & Generate Deployment Evidence (main only)
        // =====================================================================
        stage('Publish Images & Deployment Evidence') {
            when {
                branch 'main'
                expression { return env.CHANGED_MODULES != '' }
            }
            steps {
                sh '''
                    echo "$REGISTRY_CREDS_PSW" | docker login "$REGISTRY" -u "$REGISTRY_CREDS_USR" --password-stdin
                    GIT_SHA=$(git rev-parse --short HEAD)
                    IFS=',' read -ra MODULES <<< "$CHANGED_MODULES"
                    for mod in "${MODULES[@]}"; do
                      svc="${mod#services/}"
                      docker push "${IMAGE_PREFIX}/${svc}:sha-${GIT_SHA}"
                    done
                    ./platform/scripts/generate-deployment-evidence.sh "staging" "$IMAGE_PREFIX"
                '''
            }
        }

        // =====================================================================
        // STAGE 7: Manual Approval Gate -> Promote Exact Digest to Production
        // =====================================================================
        stage('Promote to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Approve promotion of verified @sha256 image digests to Production?',
                      ok: 'Promote to Production'
                sh '''
                    GIT_SHA=$(git rev-parse --short HEAD)
                    IFS=',' read -ra MODULES <<< "$CHANGED_MODULES"
                    for mod in "${MODULES[@]}"; do
                      svc="${mod#services/}"
                      docker buildx imagetools create \
                        -t "${IMAGE_PREFIX}/${svc}:production" \
                        "${IMAGE_PREFIX}/${svc}:sha-${GIT_SHA}"
                    done
                    ./platform/scripts/generate-deployment-evidence.sh "production" "$IMAGE_PREFIX"
                '''
            }
        }
    }

    post {
        always {
            // Publish JUnit XML results from both Surefire (unit) and Failsafe (integration)
            junit allowEmptyResults: true,
                  testResults: 'services/*/target/surefire-reports/*.xml, services/*/target/failsafe-reports/*.xml'

            // Archive CycloneDX SBOMs, JaCoCo reports, and Phase 23 Deployment Evidence
            archiveArtifacts allowEmptyArchive: true,
                             artifacts: 'services/*/target/classes/META-INF/sbom/application.cdx.json, target/deployment-evidence/**'
        }
    }
}
