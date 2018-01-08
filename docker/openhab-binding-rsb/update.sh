#!/bin/bash


# Pull current changes and return 0 on no change, else 1
function pulled-the-same() {
    git pull | grep -q 'Already up-to.date'
    return $?
}

cd openhab-binding-rsb
echo 'Copy the initial openhab-rsb-binding'
cp target/*.jar $prefix/share/openhab/distribution/addons/
while :
do
    if ! (pulled-the-same) then
        echo 'Perform update'
        ./install.sh --batch-mode
    fi
    exec sleep 300
done
