ARG DEBIAN_FRONTEND=noninteractive

# Install dependencies
FROM ubuntu:latest AS builder

WORKDIR /workspace

# Setup build deps
ARG DEBIAN_FRONTEND=noninteractive
RUN apt update && \
    apt install -y \
            maven \
            openjdk-11-jdk \
            git \
            rsync \
            curl \
            wget && \
    apt clean

# Setup Gradle Wrapper
COPY ../gradlew /workspace/
COPY ../gradle /workspace/gradle
RUN ./gradlew --version

# Copy repo into working directory
COPY . .


# Build jul
RUN ./init-lib.sh && \
    cd lib/jul && \
    prefix="/tmp/" ./install.sh -Dmaven.javadoc.skip=true

# Build bco
RUN ./gradlew :bco.app.util:installDist -x test -x javadoc --no-daemon --stacktrace

# Switch to baseline image
FROM ubuntu:latest

# # Install adhoc
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
    BCO_OPTIONS="" \
    JAVA_VERSION="11"

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

    
# Install java
ENV JAVA_HOME='/usr/lib/jvm/default-jvm'
# Limit JDK crypto policy by default to comply with local laws which may prohibit use of unlimited strength cryptography
RUN mkdir -p "${JAVA_HOME}" && \
    zulu8_amd64_url='https://cdn.azul.com/zulu/bin/zulu8.46.0.19-ca-jdk8.0.252-linux_x64.tar.gz' && \
    zulu8_armhf_url='https://cdn.azul.com/zulu-embedded/bin/zulu8.46.0.225-ca-jdk8.0.252-linux_aarch32hf.tar.gz' && \
    zulu8_arm64_url='https://cdn.azul.com/zulu-embedded/bin/zulu8.46.0.225-ca-jdk8.0.252-linux_aarch64.tar.gz' && \
    zulu11_amd64_url='https://cdn.azul.com/zulu/bin/zulu11.39.15-ca-jdk11.0.7-linux_x64.tar.gz' && \
    zulu11_armhf_url='https://cdn.azul.com/zulu-embedded/bin/zulu11.39.61-ca-jdk11.0.7-linux_aarch32hf.tar.gz' && \
    zulu11_arm64_url='https://cdn.azul.com/zulu-embedded/bin/zulu11.39.61-ca-jdk11.0.7-linux_aarch64.tar.gz' && \
    url_var="zulu${JAVA_VERSION}_$(dpkg --print-architecture)_url" && \
    eval "java_url=\$$url_var" && \
    echo "${java_url}" && \
    wget -nv -O /tmp/java.tar.gz "${java_url}" && \
    tar --exclude='demo' --exclude='sample' --exclude='src.zip' -xf /tmp/java.tar.gz --strip-components=1 -C "${JAVA_HOME}" && \
    if [ "${JAVA_VERSION}" = "8" ]; then \
        sed -i 's/^#crypto.policy=unlimited/crypto.policy=limited/' "${JAVA_HOME}/jre/lib/security/java.security"; \
    elif [ "${JAVA_VERSION}" = "11" ]; then \
        sed -i 's/^crypto.policy=unlimited/crypto.policy=limited/' "${JAVA_HOME}/conf/security/java.security"; \
    fi && \
    rm /tmp/java.tar.gz && \
    update-alternatives --install /usr/bin/java java "${JAVA_HOME}/bin/java" 50 && \
    update-alternatives --install /usr/bin/javac javac "${JAVA_HOME}/bin/javac" 50

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
HEALTHCHECK --interval=15m --timeout=2m CMD bco-validate >/dev/null || exit 1

# switch to root, let the entrypoint drop back to bco user
USER root

# Set command
## Need to be in array form to not encapsulate command in another shell session because
## the shell is not forwarding SIGINT and SIGTERM to bco than.
## However, in arrayw form variables can not be used!
CMD ["bco"]
