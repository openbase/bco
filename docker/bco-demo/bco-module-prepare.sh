#!/bin/bash

echo "reset db"
rm -rf /tmp/bco
ls -la /usr/share/bco/ 
cp -r /usr/share/bco /tmp/
chmod -R 777 /tmp/bco
