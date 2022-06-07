#!/bin/bash

# update top level project
git pull

# make sure submodule projects are initiated and updated
git $@ submodule init
git $@ submodule update --remote

# switch to bco master versions
git submodule foreach git checkout ${BRANCH_NAME}
git submodule foreach git pull

