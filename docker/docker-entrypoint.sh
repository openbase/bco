#!/bin/bash

# setup script environment
# set -x
set -euo pipefail

# configure script to call original entrypoint
set -- tini -s -- "$@"

# Prepare log directory
mkdir -p ${BCO_LOGS}

# Prepare bco modules if required
if [ -z ${BCO_MODULE_PREPARE_SCRIPT+x} ]; then
    echo "no module preperation required.";
else
    echo "prepare bco modules...";
    source /usr/local/bin/${BCO_MODULE_PREPARE_SCRIPT};
fi

# replace the current pid 1 with original entrypoint
echo "start main application: $@"

set -- "$@" --bco-home ${BCO_HOME} --log-dir ${BCO_LOGS} ${BCO_OPTIONS} -v
exec "$@"
