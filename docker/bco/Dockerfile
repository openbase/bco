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

# Setup Environment 
#ENV prefix /usr/local/bco
#RUN echo 'export PATH="$PATH:$prefix/bin"' >> ~/.bashrc
#RUN . ~/.bashrc
#RUN mkdir -p $prefix

# Set the working directory to the prefix
#WORKDIR $prefix

# Create RSB configuration
#RUN mkdir -p ~/.config
#ADD rsb.conf $prefix/
#RUN mv rsb.conf ~/.config/

# Create MVN configuration
#RUN mkdir -p ~/.m2
#ADD mvn-settings.xml $prefix/
#RUN mv mvn-settings.xml ~/.m2/settings.xml

#EXPOSE 4803

#VOLUME $prefix/var
#VOLUME $prefix/etc
#VOLUME $prefix/share

#RUN git clone https://github.com/openbase/bco.git bco
#RUN cd bco; ./install.sh --batch-mode; cd ..
CMD exec bco
