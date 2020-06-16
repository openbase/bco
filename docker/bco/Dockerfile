# Baseline image
FROM azul/zulu-openjdk-debian:11

# Setup Openbase Debian Repository
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys AAF438A589C2F541
RUN echo "deb https://dl.bintray.com/openbase/deb buster main" | tee -a /etc/apt/sources.list
RUN echo "deb https://dl.bintray.com/openbase/deb buster testing" | tee -a /etc/apt/sources.list


# Install bco
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    bco

# Fix UTF8
RUN export LC_ALL=en_US.UTF-8 && \
    export LANG=en_US.UTF-8 && \
    locale-gen en_US.UTF-8

#VOLUME $prefix/var
#VOLUME $prefix/etc
#VOLUME $prefix/share

# Create bco user because bco does not need any root privileges
RUN groupadd -r bco && \
    useradd --no-log-init -r -g bco bco

USER bco

CMD exec bco
