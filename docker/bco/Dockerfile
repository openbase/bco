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

# Create RSB configuration
RUN mkdir -p ~/.config
ADD rsb.conf ~/.config/rsb.conf

# Set the working directory to /app
WORKDIR $prefix

EXPOSE 4803

RUN git clone https://github.com/openbase/bco.git bco
RUN cd bco; ./install.sh; cd ..
#CMD java -jar manager.util-1.5-SNAPSHOT.one-jar.jar
