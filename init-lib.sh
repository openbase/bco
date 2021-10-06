#!/bin/bash
git $@ submodule init
git $@ submodule update --remote

# link to branch
git submodule foreach -q --recursive 'git checkout $(git config -f $toplevel/.gitmodules submodule.$name.branch || echo stable)'

