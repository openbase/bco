# Use an official Python runtime as a parent image
FROM java:8-jdk

# Install git and mvn
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    git \
    maven \
    pv

# Setup Environment 
ENV prefix /usr/local/bco
RUN echo 'export PATH="$PATH:$prefix/bin"' >> ~/.bashrc
RUN . ~/.bashrc
RUN mkdir -p $prefix

# Set the working directory to the prefix
WORKDIR $prefix

# Create MVN configuration
RUN mkdir -p ~/.m2
ADD mvn-settings.xml $prefix/
RUN mv mvn-settings.xml ~/.m2/settings.xml

VOLUME $prefix/var
VOLUME $prefix/etc
VOLUME $prefix/share

RUN git clone https://github.com/openbase/openhab-binding-rsb.git openhab-binding-rsb
RUN cd openhab-binding-rsb; ./install.sh --batch-mode; cd ..

ADD update.sh $prefix/
CMD exec ./update.sh
