#!/bin/bash
git $@ submodule init && git $@ submodule update --remote && echo "all libs are updated to its development head."
