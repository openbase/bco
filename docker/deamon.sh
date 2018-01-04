#!/bin/bash

NETWORK_NAME=BCOnet
SPREADHOST_NAME=spreadhost

# Create a new docker network to be able to alias hosts
docker network create $NETWORK_NAME

# Start the spread host
docker run -d -p 4803:4803 --network-alias=$SPREADHOST_NAME --network=$NETWORK_NAME spread
sleep 2
docker run -d --network=$NETWORK_NAME bco
