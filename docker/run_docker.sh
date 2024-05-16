#!/bin/bash

# echo -e "allow_anonymous true\nlistener 1883" > $HOME/.mosquitto.conf

# Create users and groups
# sudo adduser --system --shell /usr/sbin/nologin openhab
# sudo addgroup --system openhab
# sudo usermod -a -G openhab openhab
# sudo usermod -a -G dialout openhab
# sudo usermod -a -G tty openhab
# sudo usermod -a -G openhab ${DEFAULT_USER}

# sudo adduser --system --shell /usr/sbin/nologin bco
# sudo addgroup --system bco
# sudo usermod -a -G bco bco
# sudo usermod -a -G bco ${DEFAULT_USER}

# Set environment variables
# export USER_ID=$(id -u openhab)
# export GROUP_ID=$(getent group openhab | cut -d: -f3)
# export BCO_USER_ID=$(id -u bco)
# export BCO_GROUP_ID=$(getent group bco | cut -d: -f3)

export USER_ID=openhab
export GROUP_ID=1000
export BCO_USER_ID=1100
export BCO_GROUP_ID=1150
export OPENHAB_GROUP_ID=1200

# Run docker-compose
docker-compose -f docker-compose-copilot.yml up #--detach