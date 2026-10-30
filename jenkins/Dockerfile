# Jenkins controller image for the calculator pipeline.
# Adds the build tools the Jenkinsfile needs (Maven, Docker CLI + Compose plugin) and
# pre-installs plugins, so a fresh `docker compose up` gives a ready-to-use Jenkins.
FROM jenkins/jenkins:lts-jdk21

USER root

ARG MAVEN_VERSION=3.9.16

# Docker CLI, Buildx and Compose v2 plugin from Docker's official apt repository.
# Only the CLI is installed: builds run on the host's Docker engine through the mounted socket.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && chmod a+r /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin docker-compose-plugin \
 && rm -rf /var/lib/apt/lists/*

# Apache Maven (tarball, so the version is pinned and no extra JDK is pulled in).
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

USER jenkins

# Plugins (versions resolved to the latest compatible with this Jenkins core).
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Skip the interactive setup wizard: everything is configured by casc.yaml (JCasC).
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Duser.timezone=Asia/Kolkata" \
    CASC_JENKINS_CONFIG=/var/jenkins_conf/casc.yaml
