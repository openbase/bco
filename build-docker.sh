#!/bin/bash
set -e

NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
WHITE='\033[0;37m'

APP_NAME='docker images'
APP_NAME=${BLUE}${APP_NAME}${NC}
echo -e "=== ${APP_NAME} build docker image...${WHITE}${NC}" &&
docker build -f docker/Dockerfile -t openbaseorg/bco:local .
docker build -f docker/device-manager/openhab/Dockerfile -t openbaseorg/bco-device-manager-openhab:local --build-arg BCO_BASE_IMAGE_VERSION=local docker/device-manager/openhab
docker build -f docker/bco-demo/Dockerfile -t openbaseorg/bco-demo:local --build-arg BCO_BASE_IMAGE_VERSION=local docker/bco-demo

# use this for debugging purpose: DOCKER_BUILDKIT=0 docker build -f docker/Dockerfile --progress=plain .
echo -e "=== ${APP_NAME} were ${GREEN}successfully${NC} build.${NC}"
