---
title: BCO - Folder Structure
permalink: "/folder-structure/"
layout: default
---

# BCO Folder Structure

The following folders are used by bco:

## Installation Prefix

The default installation prefix of bco points to ``/usr`` where than the **Binary**, **Libary** and the **Shared Data** folder are based on. This default loaction is for example be used by the provided debian package to deploy the runtime enviroument. 

This prefix can individually configured via the ``$PREFIX`` system variable. Those can be set to any location with user write permissions. After defined the "./install.sh" scripts provided by each bco module are using this prefix to install bco or any submodules to the configured location. 

### Binary Folder

``/usr/bin``

Here you find all bco binaries. 

### Libary Folder

``/usr/share/bco/lib``

Is used for storing internal as well as external java libaries used by bco.

### Shared Data Folder

``/usr/share/bco``

Is used for storing database templates and other shared system resources like images or audio files.

## BCO Home

``~/.config/bco``

This folder is used for storing variable data used by bco. This includes the registry database as well as the credential store. The bco home path can be additionally referred by the global system variable ``BCO_HOME``. If defined those will be used instead of the default path.

### BCO Registry DB

``~/.config/bco/var/registry/db``

This path is based on the ``$BCO_HOME`` variable and refers to the bco registry db default location.
During startup the bco registry db location is auto detected. The lookup order is:

1. ``$BCO_HOME/var/registry/db``  (writeable if provided by file system)
2. ``$PREFIX/var/registry/db``   (writeable if provided by file system)
3. ``$PREFIX/share/registry/db`` (read only)

If no database could be found at those locations an empty one is deployed to ``$BCO_HOME``.

### Credential Store

``~/.config/bco/var/credentials``

This path is based on the ``$BCO_HOME`` variable and points to the credential store used by the bco authentication.

## Individual Configuration

The system variables``$BCO_HOME`` and ``$PREFIX`` can be used to overwrite the bco home and prefix locations. This as well as the other paths can be additioanlly overwritten via command line arguments. For this have a closer look at the help page provided by each shipped bco binary e.g ``bco --help``. 

One example could be to alternate the registry db location via ``bco --db /my/own/db``.
