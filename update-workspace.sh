#!/bin/bash
git $@ submodule init
git $@ submodule update

# switch to bco master versions
git submodule foreach git checkout master

