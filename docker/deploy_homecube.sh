#!/bin/bash

export DEFAULT_USER=$(whoami)
sudo usermod -aG docker ${DEFAULT_USER}

echo -e "allow_anonymous true\nlistener 1883" > $HOME/.mosquitto.conf

# Create users and groups
sudo adduser --system --shell /usr/sbin/nologin openhab
sudo addgroup --system openhab
sudo usermod -a -G openhab openhab
sudo usermod -a -G dialout openhab
sudo usermod -a -G tty openhab
sudo usermod -a -G openhab ${DEFAULT_USER}

sudo adduser --system --shell /usr/sbin/nologin bco
sudo addgroup --system bco
sudo usermod -a -G bco bco
sudo usermod -a -G bco ${DEFAULT_USER}

# Set environment variables
export USER_ID=$(id -u openhab)
export GROUP_ID=$(getent group openhab | cut -d: -f3)
export BCO_USER_ID=$(id -u bco)
export BCO_GROUP_ID=$(getent group bco | cut -d: -f3)

sudo chmod -R g+rwX /var/lib/docker/volumes/openhab_conf/_data/sitemaps
sudo chgrp bco /var/lib/docker/volumes/openhab_conf/_data/sitemaps

# Run docker-compose
docker compose -f docker-compose-homecube.yaml up --detach --force-recreate