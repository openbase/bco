#!/bin/bash

OPENHAB_USER_NAME="openhab"

# Prepare sitemap folder access
if [ -z ${OPENHAB_GROUP_ID+x} ]; then
    echo "openhab group id not set! Therefore, sitemap permissions can not be guaranteed.";
else
    # skip creation if already exist
    if [ -z "$(getent group $OPENHAB_GROUP_ID)" ]; then
        echo "add bco user to openhab group to guarantee sitemap folder access...";

        # create openhab group within the container
        groupadd -g ${OPENHAB_GROUP_ID} ${OPENHAB_USER_NAME}

        # register bco user as member of the openhab group
        usermod -a -G ${OPENHAB_USER_NAME} ${BCO_USER}
    fi

    # make sure openhab group can modify existing configurations
    chmod -R g+rw ${OPENHAB_CONF} 
fi
