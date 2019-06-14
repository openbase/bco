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
echo -e "=== ${APP_NAME} project ${WHITE}installation${NC}" &&
eval mvn install \
       # -DassembleDirectory=${prefix} \
        -DskipTests=true \
        -Dmaven.test.skip=true \
        -Dlicense.skipAddThirdParty=true \
        -Dlicense.skipUpdateProjectLicense=true \
        -Dlicense.skipDownloadLicenses \
        -Dlicense.skipCheckLicense=true \
        -Dmaven.license.skip=true \
        $@ $PV &&
#echo -e "\r=== ${APP_NAME} project launcher ${WHITE}installation${NC}" &&
#cp docker/bco-launcher $prefix/bin/ &&

echo -e "=== ${APP_NAME} was ${GREEN}successfully${NC} installed to ${WHITE}${prefix}${NC}"
