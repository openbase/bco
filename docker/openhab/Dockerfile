# Use an official Python runtime as a parent image
FROM java:8-jre

# Install git and mvn
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    g++ \
    gcc \
    make \
    cmake \
    git

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

# Deploy openhab runtime
ADD openhab-runtime $prefix/
RUN ./install.sh --batch-mode; cd ..
RUN rm -rf openhab-runtime
CMD exec bin/openhab
