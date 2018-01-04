# Use an official Python runtime as a parent image
FROM java:8-jre

# Install git and mvn
# RUN apt-get install git mvn

# Setup Environment 
ENV prefix /usr/local/bco
RUN echo 'export PATH="$PATH:$prefix/bin"' >> ~/.bashrc
RUN . ~/.bashrc
RUN mkdir -p $prefix

# Create RSB configuration
RUN mkdir -p ~/.config
ADD rsb.conf $prefix/
RUN mv $prefix/rsb.conf ~/.config/

# Set the working directory to /app
WORKDIR $prefix

# Add database
ADD share $prefix/share/
ADD var $prefix/var/
ADD manager.util-1.5-SNAPSHOT.one-jar.jar .

CMD java -jar manager.util-1.5-SNAPSHOT.one-jar.jar
