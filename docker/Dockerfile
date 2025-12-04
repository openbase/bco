# syntax=docker/dockerfile:1.7

############################################
# Stage 1 — Build with a lightweight JDK
############################################
ARG JAVA_VERSION=21
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /workspace

# Optional: useful for Gradle plugins that invoke git, etc.
RUN apk add --no-cache git

# Install bash
RUN apk add --no-cache bash

# Speed up builds with a persistent Gradle cache (BuildKit required)
ENV GRADLE_USER_HOME=/home/gradle/.gradle

# Now copy sources and build for real
COPY . .

# Setup Gradle Wrapper
RUN ./gradlew --version

# Build jul
RUN ./prepare.sh
RUN cd lib/jul && \
    ./install.sh

# Build bco
RUN ./gradlew :bco.app.util:installDist -x test -x javadoc --no-daemon --stacktrace


############################################
# Stage 2 — Minimal runtim
############################################
FROM eclipse-temurin:21-jre AS runtime

# # Install app
COPY --from=build /workspace/module/app/util/build/install/bco/bin /usr/bin/
COPY --from=build /workspace/module/app/util/build/install/bco/lib /usr/lib/

# Install runtime deps
## tini: required to forward app signals such as sigint.
RUN apt update && \
    apt install -y software-properties-common && \
    apt update && \
    apt install -y \
            gnupg \
            tini \
            fontconfig \
            locales \
            locales-all \
            ca-certificates \
            curl \
            git \
            wget && \
    apt clean

# Set variables and locales
ENV \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75" \
    LC_ALL="en_US.UTF-8" \
    LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    BCO_USER="bco" \
    BCO_USER_HOME="/home/bco" \
    BCO_HOME="/home/bco/data" \
    BCO_LOGS="/home/bco/data/log" \
    BCO_BINARY="/usr/bin/bco" \
    MQTT_BROKER="mqtt-broker" \
    BCO_OPTIONS=""

# Basic build-time metadata as defined at http://label-schema.org
LABEL org.label-schema.docker.dockerfile="docker/Dockerfile" \
    org.label-schema.license="GPL3" \
    org.label-schema.name="bco" \
    org.label-schema.vendor="openbase.org" \
    org.label-schema.description="A behaviour based smart environment plattform" \
    org.label-schema.url="https://www.basecubeone.org" \
    org.label-schema.vcs-type="Git" \
    org.label-schema.vcs-url="https://github.com/openbase/bco.git" \
    io.hass.type="addon" \
    io.hass.arch="aarch64|amd64" \
    maintainer="Divine Threepwood <divine@openbase.org>"

# Expose volume
VOLUME ${BCO_HOME}

# Set working dir
WORKDIR ${BCO_USER_HOME}

# Set entry point
# entrypoint is used to update docker gid and revert back to bco user
COPY docker/docker-entrypoint.sh /usr/local/bin/
RUN ln -s /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

# Configure healthcheck
# todo: make sure only the registry availability is checks since devices are not maintained by this instance.
HEALTHCHECK --interval=15m --timeout=2m CMD bco-validate --host ${MQTT_BROKER} > /dev/null || exit 1

# Set command
## Need to be in array form to not encapsulate command in another shell session because
## the shell is not forwarding SIGINT and SIGTERM to bco than.
## However, in arrayw form variables can not be used!
CMD ["bco"]
