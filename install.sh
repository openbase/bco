#!/bin/bash

NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
WHITE='\033[0;37m'

export BCO_DIST="${BCO_DIST:=$HOME/usr/}"

if [ ! -d ${BCO_DIST} ]; then
    echo "No bco distribution found at: ${BCO_DIST}"
    echo 'Please define the distribution installation target directory by setting the $BCO_DIST environment variable.'
    exit 255
fi

APP_NAME='bco'
APP_NAME=${BLUE}${APP_NAME}${NC}
echo -e "=== ${APP_NAME} project ${WHITE}cleanup${NC}" &&
./gradlew clean --quiet $@ &&
echo -e "=== ${APP_NAME} project ${WHITE}installation${NC}" &&
./gradlew \
    deploy-bco-dist \
    publishToMavenLocal \
    --exclude-task test \
    --exclude-task javaDoc \
    --parallel \
    --quiet \
    $@ &&
echo -e "=== ${APP_NAME} was ${GREEN}successfully${NC} installed to ${WHITE}${BCO_DIST}${NC}"
