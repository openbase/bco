#!/bin/bash

# setup script environment
# set -x
set -euo pipefail

# configure script to call original entrypoint
set -- tini -s -- "$@"

# Prepare log directory
mkdir -p "${BCO_LOGS}"

# Prepare bco modules if required
if [ -z ${BCO_MODULE_PREPARE_SCRIPT+x} ]; then
    echo "no module preperation required.";
else
    echo "prepare bco modules...";
    # shellcheck disable=SC1090  # source a dynamic script path intentionally
    source /usr/local/bin/"${BCO_MODULE_PREPARE_SCRIPT}";
fi

# replace the current pid 1 with original entrypoint
# Use printf to safely print all positional args without mixing scalar and array expansions
printf 'start main application: %s\n' "$*"

# If BCO_OPTIONS contains multiple space-separated arguments, expand them as distinct
# arguments by splitting into a bash array. This avoids passing the entire string as
# a single argument when using "${BCO_OPTIONS}".
_bco_options_array=()
if [ -n "${BCO_OPTIONS:-}" ]; then
    # read -a splits on IFS (whitespace), preserving each option as a separate element
    # Note: if options themselves need to contain spaces, provide them via an array env
    # or a different mechanism; this simple split handles the common case of multiple flags.
    read -r -a _bco_options_array <<< "${BCO_OPTIONS}"
fi

set -- "$@" --bco-home "${BCO_HOME}" --log-dir "${BCO_LOGS}" --host "${MQTT_BROKER}" "${_bco_options_array[@]}"
exec "$@"
