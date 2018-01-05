#!/bin/bash

# Make sure only root can run this script
if [[ $EUID -ne 0 ]]; then
    echo "This script must be executed with root permissions" 1>&2
    exit 1
fi

echo "### Start daemon installation routine. ###"

echo "### Delpoy daemon..."
cp -i bco.init.d /etc/init.d/bco

echo "### Setup daemon autostart..."
update-rc.d bco defaults

echo "### Daemon installation finish!"
exit
