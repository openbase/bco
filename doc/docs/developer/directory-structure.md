---
sidebar_position: 3
---

# Directory Structure

The following directories are used by BCO:

## Installation Distribution Path

The default installation prefix of bco points to ``$HOME/usr/`` where than the **Binary**, **Library** and the **Shared Data** folder are based on.
This default location is for example be used by the provided debian package to deploy the runtime environment. 

This prefix can individually configured via the ``$BCO_DIST`` system variable.
Those can be set to any location with user write permissions.
After defined the ``./install.sh`` scripts provided by each bco module are using this prefix to install bco or any submodules to the configured location. 

### Binary Folder

``$HOME/usr/bin``

Here you find all bco binaries. 

### Libary Folder

``$HOME/usr/share/bco/lib``

Is used for storing internal as well as external java libraries used by bco.

### Shared Data Folder

``$HOME/usr/share/bco``

Is used for storing database templates and other shared system resources like images or audio files.

## BCO Home

``$HOME/.config/bco``

This folder is used for storing variable data used by bco. This includes the registry database as well as the credential store.
The bco home path can be additionally referred by the global system variable ``BCO_HOME``.
If defined those will be used instead of the default path.

### BCO Registry DB

``$HOME/.config/bco/var/registry/db``

This path is based on the ``$BCO_HOME`` variable and refers to the BCO Registry database default location.
During startup the database directory is auto detected. The lookup will be performed in the following order:

1. ``$BCO_HOME/var/registry/db``  (writeable if provided by file system)
2. ``$BCO_DIST/var/registry/db``   (writeable if provided by file system)
3. ``$BCO_DIST/share/registry/db`` (read only)

If no database could be found at those locations an empty one is deployed to ``$BCO_HOME``.

### Credential Store

``$HOME/.config/bco/var/credentials``

This path is based on the ``$BCO_HOME`` variable and points to the credential store used by the bco authentication.

## Individual Configuration

The system variables``$BCO_HOME`` and ``$BCO_DIST`` can be used to overwrite the bco home and prefix locations.
This as well as the other paths can be additionally overwritten via command line arguments.
For this have a closer look at the help page provided by each shipped bco binary e.g ``bco --help``.
One example could be to alternate the registry db location via ``bco --db /my/own/db``.
