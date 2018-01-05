# Use an official Python runtime as a parent image
FROM java:8-jre

# Install git and mvn
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
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

# Deplay openhab runtime
RUN git clone https://projects.cit-ec.uni-bielefeld.de/git/lsp-csra.openhab-runtime openhab-runtime
RUN cd openhab-runtime; ./install.sh --batch-mode; cd ..
CMD exec bin/openhab
