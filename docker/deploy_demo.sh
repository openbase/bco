#!/bin/bash

echo -e "allow_anonymous true\nlistener 1883" > $HOME/.mosquitto.conf

docker compose -f docker-compose-demo.yaml up -d --force-recreate