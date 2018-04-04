---
title: BCO - Folder Structure
permalink: "/folder-structure/"
layout: default
---

# BCO Folder Structure

The following folders are used by bco:

## Runtime System Folder

### Binaries

``/usr/bin``

Here you find all bco binaries.

### Libaries

``/usr/share/maven-repo``

Is used for storing internal as well as external java libaries used by bco.

### Shared Data

``/usr/share``

Is used for storing database templates and other shared system resources like images or audio files.

## BCO Home

``~/.config/bco``

This folder is used for storing variable data used by bco. This includes the registry database as well as the credential store.
The bco home path can be additionally referred by the global system variable ``BCO_HOME``. If defined those will be used instead of the default path.

### BCO Registry DB

``~/.config/bco/var/registry``

This path is based on the ``BCO_HOME`` variable and refers to the bco registry db.

### Credential Store

``~/.config/bco/var/credentials``

This path is also based on the ``BCO_HOME`` and points to the credential store used by the bco authentication.

## Outdated

In the past the variable ``prefix`` was used for the bco home path detection. Please only use ``BCO_HOME`` to specify the bco config folder.  
