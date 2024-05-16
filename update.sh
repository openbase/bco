#!/bin/bash

# update top level project
git pull

# make sure submodule projects are initiated and linked to its compatible hashes
git $@ submodule init
git $@ submodule update

git submodule foreach ./install.sh
./install.sh
