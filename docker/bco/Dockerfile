# Use an official Python runtime as a parent image
FROM java:8-jre

# Install git and mvn
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    git \
    maven

# Setup Environment 
ENV prefix /usr/local/bco
RUN echo 'export PATH="$PATH:$prefix/bin"' >> ~/.bashrc
RUN . ~/.bashrc
RUN mkdir -p $prefix

# Set the working directory to the prefix
WORKDIR $prefix

# Create RSB configuration
RUN mkdir -p ~/.config
ADD rsb.conf $prefix/
RUN mv rsb.conf ~/.config/

# Create MVN configuration
RUN mkdir -p ~/.m2
ADD mvn-settings.xml $prefix/
RUN mv mvn-settings.xml ~/.m2/settings.xml

EXPOSE 4803

RUN git clone https://github.com/openbase/bco.git bco
RUN cd bco; ./install.sh --batch-mode; cd ..
CMD bin/bco
