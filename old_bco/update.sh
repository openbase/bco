#!/bin/bash

# update top level project
git pull

# make sure submodule projects are initiated and updated
git $@ submodule init
git $@ submodule update --remote

# detect project branch
BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)

# switch to bco master versions
git submodule foreach git checkout ${BRANCH_NAME}
git submodule foreach git pull

