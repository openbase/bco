#!/bin/bash

NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
WHITE='\033[0;37m'

APP_NAME='bco'
APP_NAME=${BLUE}${APP_NAME}${NC}

if [ -x "$(command -v pv)" ]; then
    PV='| pv --line-mode -p >> /dev/null'
else
    PV='--quiet'
fi


echo -e "=== ${APP_NAME} project ${WHITE}cleanup${NC}" &&
mvn clean --quiet $@ &&
echo -e "=== ${APP_NAME} project ${WHITE}build${NC}" &&
eval mvn install package $@ $PV &&
echo -e "=== ${APP_NAME} was ${GREEN}successfully${NC} build to ${WHITE}${prefix}${NC}"
