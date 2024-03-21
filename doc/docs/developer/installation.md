---
sidebar_position: 1
---

# Developer Installation Guide

## Supported Platforms
* BCO Runtime
  * Ubuntu / Debian
* BCO UIs
  * Ubuntu / Debian / Linux
  * Windows 
  * Mac OS

## Requirements

* Java JDK 11 (AZUL Zulu JDK recommended)
    * Download: [https://www.azul.com/downloads/zulu](https://www.azul.com/downloads/zulu)
    * Ubuntu Install Example
      * Add Key: ```sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9```
      * Add Repo: ```sudo apt-add-repository 'deb http://repos.azulsystems.com/ubuntu stable main'```
      * Update Index: ```sudo apt-get update```
      * Install: ```sudo apt-get install zulu-11```
      * Make Default: ```echo 'export JAVA_HOME="/usr/lib/jvm/zulu-11-amd64/"' >> ~/.bashrc && . ~/.bashrc```
* Git
    * ```sudo apt-get install git```

##  Toolchain Setup

Define where to install the bco distribution via the `BCO_DIST` variable.
```
echo 'export BCO_DIST="$HOME/usr/bco"' >> ~/.bashrc
```
Add the ```bin``` folder to your global ```$PATH``` variable to support direct binary execution.
```
echo 'export PATH="$PATH:$BCO_DIST/bin"' >> ~/.bashrc
```
Reload your bash configuration
```
. ~/.bashrc
```
Make sure the ```$BCO_DIST``` folder exists.
```
mkdir -p $BCO_DIST
```
Make sure you have right permissions to ```$BCO_DIST```
```
sudo chown -R $USER $BCO_DIST
chmod -R 750 $BCO_DIST
```

## BCO Installation

After preparing the toolchain and middleware we need to prepare our workspace.
Create a new development directory if it not already exist (e.g. ``~/workspace/openbase``) and change into these directory.
```
mkdir -p ~/workspace/openbase
cd ~/workspace/openbase
```

### Openbase Developer Tool Installation

There exist different openbase tools which help to to simplify the installation and maintenance of bco.
Therefore, before downloading bco we should install those tools via:
```
cd ~/workspace/openbase
git clone -b stable https://github.com/openbase/developer.tools.git
cd developer.tools
./install.sh
```

### Main Repository Download 

Download the bco main repository into your development workspace.
```
cd ~/workspace/openbase
git clone -b stable https://github.com/openbase/bco.git
```
:::info INFO
We recommend to checkout and install the ```dev``` branch in order to start the development of new components.
:::

### Download and Prepare BCO Submodules

BCO is based on the openbase Java Utility Library (JUL) and on the openbase Type Library. Both dependencies are linked as git sub-module in the `lib` folder. The ```./prepare.sh``` stored in the BCO repo root folder downloads the source-code of each dependency.
Therefore, please execute
```
cd ~/workspace/openbase/bco
./prepare.sh
./update.sh
```

If the workspace is prepared, we can build bco by using the ```install``` script:
```
cd ~/workspace/openbase/bco
./install.sh
```
:::info INFO
The initial installation can take a while, so grab a coffee and relax while the scripts do the work.
:::

Now everything should be ready to start the development of new bco components and apps. We recommend to use IntelliJ as IDE for BCO.
Open ```~/workspace/openbase/bco``` in the IDE or just execute ```idea ~/workspace/openbase/bco``` in case IntelliJ is provided by your shell.

### Update

You can update bco including its dependencies by executing ```./update.sh```. Just make sure all local changes are committed and pushed before performing the update. After updating all components you can compile and install all changes via the ```./install.sh``` script. Therefore, a full update can be performed as followed:
```
./update.sh
./install.sh
```

## Database Setup

A fresh and empty database is generated during the first start of BCO.
This database is placed at ```~/.config/bco/var/registry/db``` and only contains a root location and some system user accounts as well as the default admin account.
:::warning INFO
Please do not modify any database entries by hand as long as you exactly know what you are doing since manual modifications can lead into an inconsistent database.
:::
Once bco is started you can add further units by adding them via the ``bco-registry-editor``. User accounts can be created and passwords changed via ```bco-console```. 

In general bco takes care of all class and template database entries. Those will be updated during each startup of bco as long as an internet connection is provided.
In case you want to backup your individual setup entries just create a local git repository in the database folder ```git init```, link it to any git remote repository of your choice ```git remote add origin https://github.com/$YOUR_ACCOUNT/bco.registry.$YOUR_HOME_ID-db.git```, and upload the db via ```git push --set-upstream origin main```.

### How to setup a Demo Database
Sometimes during development its useful to play around with an already complex environment setup.
The following steps explain how to setup the bco demo database that already entails a bunch of devices and units. 
```
mkdir -p ~/.config/bco/var/registry
cd ~/.config/bco/var/registry
git clone https://github.com/openbase/bco.registry.demo-db db
```

### How to Restore a Backup

To restore an already existing bco setup, just place the related ```db``` directory within ```~/.config/bco/var/registry```.