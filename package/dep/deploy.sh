#!/bin/bash

NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
WHITE='\033[0;37m'

APP_NAME='bco'
APP_NAME=${BLUE}${APP_NAME}${NC}
VERSION=2.0.0-alpha.1
DESCRIPTION='The experimental alpha prerelease of bco 2.0'
DISTRIBUTION=wheezy,stretch,bionic,buster
SCM=https://github.com/openbase/bco.git
ARCHITECTURE=all,mips,armhf,arm64,armel,i386,amd64
COMPONENT=unstable,testing
#COMPONENT=main,free,unstable,testing
USER=divinethreepwood
API_KEY= <todo: generate via https://bintray.com/profile/edit and insert>
FILE_TARGET_PATH=pool/main/b/bco/bco_$VERSION.deb
SOURCE_FILE_PATH=target/bco_2.0\~*.deb
BINTRAY_REPO=https://api.bintray.com/content/openbase/deb/bco

echo -e "=== ${APP_NAME} project ${WHITE}upload${NC}" &&
eval curl -T $(SOURCE_FILE_PATH) -u$(USER):$(API_KEY) $(BINTRAY_REPO)/$VERSION/$(FILE_TARGET_PATH);deb_distribution=${DISTRIBUTION};deb_component=${COMPONENT};deb_architecture=${ARCHITECTURE}
echo -e "=== ${APP_NAME} was ${GREEN}successfully${NC} build to ${WHITE}${prefix}${NC}"