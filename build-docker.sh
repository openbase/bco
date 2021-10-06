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
echo -e "=== ${APP_NAME} are building${WHITE}${NC}" &&
docker build -f docker/Dockerfile .
docker build -f docker/device-manager/openhab/Dockerfile docker/device-manager/openhab
docker build -f docker/bco-demo/Dockerfile docker/bco-demo

# use this for debugging purpose: DOCKER_BUILDKIT=0 docker build -f docker/Dockerfile --progress=plain .
echo -e "=== ${APP_NAME} were ${GREEN}successfully${NC} build.${NC}"
