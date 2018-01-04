# Use an official debian as parent image
FROM buildpack-deps:jessie-curl

# Install spread
RUN echo 'deb http://packages.cor-lab.de/ubuntu/ xenial main' | tee -a /etc/apt/sources.list
RUN echo 'deb http://packages.cor-lab.de/ubuntu/ xenial testing' | tee -a /etc/apt/sources.list
RUN wget -q http://packages.cor-lab.de/keys/cor-lab.asc -O- | apt-key add -
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    spread \
    librsbspread0.15

# Make spread port available
EXPOSE 4803

# Create spread user because spread will not start as root
RUN useradd -ms /bin/bash spread
USER spread
WORKDIR /home/spread

# Start spread using the default configuration
CMD spread
