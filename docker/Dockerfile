# Install dependencies
FROM eclipse-temurin:17-jdk AS builder

ENV \
    JAVA_OPTS="" \
    LC_ALL="en_US.UTF-8" \
    LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8"

WORKDIR /workspace

# Setup build deps
ARG DEBIAN_FRONTEND=noninteractive
RUN apt update && \
    apt install -y \
            git \
            gradle \
            rsync \
            curl \
            ca-certificates \
            locales \
            locales-all \
            wget && \
    apt clean

# Copy repo into working directory
COPY . .

# Setup Gradle Wrapper
RUN ./gradlew --version

# Build jul
RUN ./prepare.sh
RUN cd lib/jul && \
    ./install.sh

# Build bco
RUN ./gradlew :bco.app.util:installDist -x test -x javadoc --no-daemon --stacktrace

# Switch to baseline image
FROM eclipse-temurin:17-jre

# # Install app
COPY --from=builder /workspace/module/app/util/build/install/bco/bin /usr/bin/
COPY --from=builder /workspace/module/app/util/build/install/bco/lib /usr/lib/
# COPY --from=builder /workspace/build/share /usr/share/

# Install runtime deps
## gosu: required to switch to another user ofter entrypoint is started as root.
## tini: required to forward app signals such as sigint.
RUN apt update && \
    apt install -y software-properties-common && \
    apt update && \
    apt install -y \
            gosu \
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
    JAVA_OPTS="" \
    LC_ALL="en_US.UTF-8" \
    LANG="en_US.UTF-8" \
    LANGUAGE="en_US.UTF-8" \
    BCO_USER="bco" \
    BCO_USER_HOME="/home/bco" \
    BCO_HOME="/home/bco/data" \
    BCO_LOGS="/home/bco/data/log" \
    BCO_BINARY="/usr/bin/bco" \
    MQTT_BROKER="mqtt-broker" \
    BCO_OPTIONS="--host ${MQTT_BROKER}"

# Basic build-time metadata as defined at http://label-schema.org
LABEL org.label-schema.build-date=$BUILD_DATE \
    org.label-schema.docker.dockerfile="docker/Dockerfile" \
    org.label-schema.license="GPL3" \
    org.label-schema.name="bco" \
    org.label-schema.vendor="openbase.org" \
    org.label-schema.version=$VERSION \
    org.label-schema.description="A behaviour based smart environment plattform" \
    org.label-schema.url="https://www.basecubeone.org" \
    org.label-schema.vcs-ref=$VCS_REF \
    org.label-schema.vcs-type="Git" \
    org.label-schema.vcs-url="https://github.com/openbase/bco.git" \
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

# TODO: probably deprecated by setting the variables BCO_LOGS above
# Configure logger
# COPY logback-debug.xml /etc/bco/
# COPY logback.xml /etc/bco/

# Configure healthcheck
# todo: make sure only the registry availability is checks since devices are not maintained by this instance.
HEALTHCHECK --interval=15m --timeout=2m CMD bco-validate --host ${MQTT_BROKER} > /dev/null || exit 1

# switch to root, let the entrypoint drop back to bco user
USER root

# Set command
## Need to be in array form to not encapsulate command in another shell session because
## the shell is not forwarding SIGINT and SIGTERM to bco than.
## However, in arrayw form variables can not be used!
CMD ["bco"]
