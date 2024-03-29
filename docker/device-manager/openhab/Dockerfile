# Baseline image
ARG BCO_BASE_IMAGE_VERSION=latest-amd64
FROM openbaseorg/bco:${BCO_BASE_IMAGE_VERSION}

# Set variables and locales
ENV \
    JAVA_OPTS="" \
    OPENHAB_CONF="/etc/openhab2" \
    BCO_MODULE_PREPARE_SCRIPT="bco-module-prepare.sh"

# Basic build-time metadata as defined at http://label-schema.org
LABEL org.label-schema.build-date=$BUILD_DATE \
    org.label-schema.docker.dockerfile="/docker/device-manager/openhab/Dockerfile" \
    org.label-schema.license="GPL3" \
    org.label-schema.name="bco-device-manager-openhab" \
    org.label-schema.vendor="openbase.org" \
    org.label-schema.version=$VERSION \
    org.label-schema.description="The BCO Device Manager connects all openhab devices to the bco infrastructure." \
    org.label-schema.url="https://www.basecubeone.org" \
    org.label-schema.vcs-ref=$VCS_REF \
    org.label-schema.vcs-type="Git" \
    org.label-schema.vcs-url="https://github.com/openbase/bco.git" \
    maintainer="Divine Threepwood <divine@openbase.org>"

# Expose volume
VOLUME ${BCO_HOME} ${OPENHAB_CONF}

# Set custom prepare script
COPY ${BCO_MODULE_PREPARE_SCRIPT} /usr/local/bin/
RUN ln -s /usr/local/bin/${BCO_MODULE_PREPARE_SCRIPT} && \
    chmod +x /usr/local/bin/${BCO_MODULE_PREPARE_SCRIPT}

# Configure healthcheck
# todo: make sure only device offered by this device manager are checked
HEALTHCHECK --interval=15m --timeout=2m CMD bco-validate  --host ${MQTT_BROKER} >/dev/null || exit 1

# switch to root, let the entrypoint drop back to bco user
USER root

# Set command
CMD ["bco-manager-device-openhab"]
