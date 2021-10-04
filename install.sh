#!/bin/bash

NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
WHITE='\033[0;37m'

APP_NAME='bco'
APP_NAME=${BLUE}${APP_NAME}${NC}
echo -e "=== ${APP_NAME} project ${WHITE}cleanup${NC}" &&
./gradlew clean --quiet $@ &&
echo -e "=== ${APP_NAME} project ${WHITE}installation${NC}" &&
./gradlew \
    build \
    testDeploy \
    --exclude-task test \
    --exclude-task javaDoc \
    --parallel \
    --quiet \
    $@ &&
echo -e "=== ${APP_NAME} was ${GREEN}successfully${NC} installed to ${WHITE}${prefix}${NC}"
