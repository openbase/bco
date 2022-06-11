#!/bin/bash

# update top level project
git pull

# make sure submodule projects are initiated and updated
git $@ submodule init
git $@ submodule update --remote

git submodule foreach ./install.sh
./install.sh
