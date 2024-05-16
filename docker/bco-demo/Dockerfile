ARG BCO_BASE_IMAGE_VERSION=latest-amd64

# Install dependencies
FROM ubuntu:latest AS builder

WORKDIR /workspace

# Setup build deps
ARG DEBIAN_FRONTEND=noninteractive
RUN apt update && \
    apt install -y \
            git && \
    apt clean

# Download demo db
RUN git clone https://github.com/openbase/bco.registry.demo-db.git db

# Baseline image

FROM openbaseorg/bco:${BCO_BASE_IMAGE_VERSION}

# Set variables and locales
ENV \
    JAVA_OPTS="" \
    OPENHAB_CONF="/etc/openhab2" \
    BCO_OPTIONS="--db /tmp/bco/db --host ${MQTT_BROKER}" \
    BCO_MODULE_PREPARE_SCRIPT="bco-module-prepare.sh"

# Basic build-time metadata as defined at http://label-schema.org
LABEL org.label-schema.build-date=$BUILD_DATE \
    org.label-schema.docker.dockerfile="/docker/bco-demo/Dockerfile" \
    org.label-schema.license="GPL3" \
    org.label-schema.name="bco-test" \
    org.label-schema.vendor="openbase.org" \
    org.label-schema.version=$VERSION \
    org.label-schema.description="The BCO Demo downloads the demo db and starts bco in test mode." \
    org.label-schema.url="https://www.basecubeone.org" \
    org.label-schema.vcs-ref=$VCS_REF \
    org.label-schema.vcs-type="Git" \
    org.label-schema.vcs-url="https://github.com/openbase/bco.git" \
    maintainer="Divine Threepwood <divine@openbase.org>"

# Set custom prepare script
COPY ${BCO_MODULE_PREPARE_SCRIPT} /usr/local/bin/
RUN ln -s /usr/local/bin/${BCO_MODULE_PREPARE_SCRIPT} && \
    chmod +x /usr/local/bin/${BCO_MODULE_PREPARE_SCRIPT}

# # Install db
RUN mkdir -p /usr/share/bco/
COPY --from=builder /workspace/db /usr/share/bco/db/

# Configure healthcheck
# todo: make sure only device offered by this device manager are checked
HEALTHCHECK --interval=15m --timeout=2m CMD bco-validate --host ${MQTT_BROKER} >/dev/null || exit 1

# switch to root, let the entrypoint drop back to bco user
USER root

# Set command
CMD ["bco-test"]
