#!/bin/bash

SPREAD_DOCKER_NAME=spread
BCO_DOCKER_NAME=bco

cd spread && docker build -t $SPREAD_DOCKER_NAME . && cd ..
cd bco && docker build -t $BCO_DOCKER_NAME . && cd ..
