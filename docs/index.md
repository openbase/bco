---
title: "{}"
layout: default
---

# Software Architecture

## Domotic Abstraction Layer (DAL)

![DALayer](images/DALayer.png)

### Unit Types

![UnitClassDiagramm](images/UnitClassDiagramm.png)

## Location Architecture

![LocationClassStructure]({{ site.baseurl }}/images/LocationArchitecture_ClassStructure.png)

![LocationHierarchicalStructure]({{ site.baseurl }}/images/LocationArchitecture_HierarchicalStructure.png)

![LocationGraphStructure]({{ site.baseurl }}/images/LocationArchitecture_GraphStructure.png)

[More Details]({{ site.baseurl }}/location-architecture)

# Software Components

![GUI Overview](images/BCO_Architecture_Paramite.png)

## Core Framework

### BCO DAL
[![Build Status](https://travis-ci.org/openbase/bco.dal.svg?branch=master)](https://travis-ci.org/openbase/bco.dal?branch=master)
[![Build Status](https://travis-ci.org/openbase/bco.dal.svg?branch=latest-stable)](https://travis-ci.org/openbase/bco.dal?branch=latest-stable)

#### Maven Artifact
```xml
<dependency>
    <groupId>org.openbase.bco</groupId>
    <artifactId>dal.remote</artifactId>
    <version>[1.6,1.7-SNAPSHOT)</version>
</dependency>
```

Repository: <https://github.com/openbase/bco.dal.git>

### BCO Registry
[![Build Status](https://travis-ci.org/openbase/bco.registry.svg?branch=master)](https://travis-ci.org/openbase/bco.registry?branch=master)
[![Build Status](https://travis-ci.org/openbase/bco.registry.svg?branch=latest-stable)](https://travis-ci.org/openbase/bco.registry?branch=latest-stable)

#### Maven Artifact
```xml
<dependency>
    <groupId>org.openbase.bco</groupId>
    <artifactId>registry</artifactId>
    <version>[1.6,1.7-SNAPSHOT)</version>
</dependency>
```

Repository: <https://github.com/openbase/bco.registry.git>

### BCO Manager
[![Build Status](https://travis-ci.org/openbase/bco.manager.svg?branch=master)](https://travis-ci.org/openbase/bco.manager?branch=master)
[![Build Status](https://travis-ci.org/openbase/bco.manager.svg?branch=latest-stable)](https://travis-ci.org/openbase/bco.manager?branch=latest-stable)

#### Maven Artifact
```xml
<dependency>
    <groupId>org.openbase.bco</groupId>
    <artifactId>manager</artifactId>
    <version>[1.6,1.7-SNAPSHOT)</version>
</dependency>
```

Repository: <https://github.com/openbase/bco.manager.git>


## Developer Tools
```
bco-registry-editor
```
```
bco-print-registry
```
```
bco-print-interface-rsb
```
```
bco-query
```
```
bco-scene-editor
```
```
bco-visual-remote
```

## Simulation Mode

You can test and prove new components in a simulated environment.
```
bco --simulate
```

## Benchmark Mode

You can test new components via the benchmark mode to prove how your components act during massive bco system load. 
During the benchmark, bco will start in simulation mode and high frequenly unit state changes are generated.

Note: Please never start the benchmark mode if you are connected to any physical devices to avoid hardware damage.
```
bco --benchmark
```

## User Interfaces

### Desktop (JavaFX)
```
bcozy
```
* <https://github.com/openbase/bco.bcozy/wiki>

### Android
bcomfy
* <https://github.com/openbase/bco.bcomfy/wiki>

# Used Libaries

## Libraries from openBase

* JPS [https://github.com/openbase/jps](https://github.com/openbase/jps)
    * A command-line argument parser and application property management framework.
* JUL [https://github.com/openbase/jul](https://github.com/openbase/jul)
    * A java utility library.

## Libraries from Citec (University of Bielefeld)

* RSB <http://docs.cor-lab.de//rsb-manual/0.15/html/examples.html>
    * The middleware used for platform independent network communication.
* RST <http://docs.cor-lab.de//rst-manual/0.15/html/data-types.html>
    * The data type library based on google protocol-buffers.
        * https://developers.google.com/protocol-buffers/
    * Repositories
        * Stable & Sandbox
            * <https://code.cor-lab.de/git/rst.git.proto>
        * Experimental (Only reachable from the university of bielefeld network)
            * <http://projects.cit-ec.uni-bielefeld.de/git/rst-experimental.proto.git>
                * BCO Types are placed in ```proto/experimental/rst/domotic```
                
# Development Installation Guide - Ubuntu / Debian

## Reqirements

* Java JDK 8 (Oracle JDK recommented)
    * Debian: <http://www.webupd8.org/2014/03/how-to-install-oracle-java-8-in-debian.html>
    * Ubuntu: <https://wiki.ubuntuusers.de/Java/Installation/Oracle_Java/Java_8/>
* Maven
    * ```sudo apt-get install maven```
* Git
    * ```sudo apt-get install git```

##  Environment Setup

Define where to install the bco distribution via the prefix variable.
```
echo 'export prefix="$HOME/local/bco"' >> ~/.bashrc
```
Add the ```bin``` folder to your global ```$PATH``` variable to support direct binary execution.
```
echo 'export PATH="$PATH:$prefix/bin"' >> ~/.bashrc
```
Reload your bash configuration
```
. ~/.bashrc
```
Make sure the ```$prefix``` folder exists.
```
mkdir -p $prefix
```
Make sure you have right permissions to ```$prefix```
```
sudo chown -R $USER $prefix
chmod -R 750 $prefix
```

## Setup Cor-Lab Debian Repository

This repository provides a collection of precompiled libs and tools for rsb. This includes transport layers like spread as well as dev-libs for using rsb in python or c++. To register the repository to your local debian package manager follow the instructions on [http://packages.cor-lab.de/](http://packages.cor-lab.de/)
* example for ubuntu xenial
  ```
  echo 'deb http://packages.cor-lab.de/ubuntu/ xenial main' | sudo tee -a /etc/apt/sources.list
  echo 'deb http://packages.cor-lab.de/ubuntu/ xenial testing' | sudo tee -a /etc/apt/sources.list
  wget -q http://packages.cor-lab.de/keys/cor-lab.asc -O- | sudo apt-key add -
  sudo apt-get update
  ```

## Spread Installation

[Spread](http://www.spread.org/download.html) is the recommended and most stable transport protocol for bco.

* Installation via Cor-Lab Debian Repository
    * ```sudo apt-get install spread librsbspread0.17```
* Official Installation Guide
    * <http://www.spread.org/download.html>
    
## RSB Libs and Tools (Optional)

The rsb python and c++ libs can be installed via the cor-lab debian repository as well as the rsb developer tools:
```
sudo apt-get install librsc0.17 librsb0.17 rsb0.17 rst0.17 cl-rsb rsb-tools-cpp0.18 rsb-tools-cl0.18
```

## RSB Configuration

Create the configuration file ```touch ~/.config/rsb.conf``` and add the following lines to deactivate the socket and enable the spread transport protocol. 
```
[transport.socket]
enabled = 0
    
[transport.spread]
enabled = 1
host    = localhost
``` 
If your bco runtime is hosted on another mschine than your local one and there is already a spread daemon running, just refere to this host instead linking to your local host.
```
#host    = localhost
host    = 192.168.x.x
```

## BCO Installation

Create a new developer directory if it not already exists (e.g. ``~/workspace/openbase``) and change into these directory.
```
mkdir -p ~/workspace/openbase
cd ~/workspace/openbase
```

## Repository Download 

Download the bco core repository into your development workspace:
```
cd ~/workspace/openbase
git clone -b latest-stable https://github.com/openbase/bco.git
```
This core repository provides all binaries and libaries. If you plan to extend or bugfix any BCO core components, you can download all submodules (exclusive for bco development) via the following command:
```
cd ~/workspace/openbase/bco
./workspace-prepare.sh
```

## Installation

Now, you should be able to start the installation. During this, all bco core components are installed to the previously defined ```$prefix```. To perform the installation (or update the components later on) execute the installation script provided by the bco folder.
```
./install.sh
```

## Setup Registry DB

Download the example db.
```
mkdir -p ~/.config/bco/var/registry
cd ~/.config/bco/var/registry
git clone https://github.com/csra/bco.registry.csra-db db
```

## How to start BCO

### Provide a Spread Daemon

First of all we need at least one running spread daemon in your network to provide the communication between all distributed bco components. You can choose to start your own spread daemon or to connect to an already running instance.

* Start a new spread daemon on your localhost machine
    * just execute ```spread```
* Connect to an already running instance
    * Make sure the external spread host is properly defined in the rsb configuration. 
        * Example: If there is a spread instance running on host ```spider``` your config file should provide the following entry.
            * ```~/.config/rsb.conf``` should contain ```host    = spider```

### Start BCO

You can start the bco runtime with the following command:

```
bco
```

# Code Examples

This examples can be used as kick-off before jumping into the bco development. They pick up the basic functions like how to query, control and access any units.

Note: Please make sure spread and bco are started within your network before executing the howtos.

## Java DAL Remote

For running any java examples you only need to include the dal remote dependency in your maven or gradle project description:

```xml
<dependency>
    <groupId>org.openbase.bco</groupId>
    <artifactId>dal.remote</artifactId>
    <version>[1.6,1.7-SNAPSHOT)</version>
</dependency>
```

### How to activate a scene
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToActivateASceneViaDAL.java)

Request the unit
```java
    LOGGER.info("request the scene with the label \"WatchingTV\"");
    testScene = Units.getUnitByLabel("WatchingTV", true, Units.SCENE);
```
Control the unit
```java
    LOGGER.info("activate the scene");
    testScene.setActivationState(ActivationState.State.ACTIVE);
```

### How to control a colorable light 
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToControlAColorableLightUnitViaDAL.java)

Request the unit
```java
    LOGGER.info("request the light unit with the label \"TestUnit_0\"");
    testLight = Units.getUnitByLabel("TestUnit_0", true, Units.LIGHT_COLORABLE);
```
Control the unit
```java
    LOGGER.info("switch the light on");
    testLight.setPowerState(PowerState.State.ON);
    
    LOGGER.info("switch light color to blue");
    testLight.setColor(Color.BLUE);
```

### How to observe a location specific reed contact 
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToObserveLocationSpecificReedContactsViaDAL.java)

### How to observe service state changes 
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToObserveServiceStateChangesViaDAL.java)

## Java RSB

### How to observe service state changes 
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/java/org/openbase/bco/dal/example/HowToObserveServiceStateChangesViaRSB.java)

## Python RSB

### How to request, control and observe units
* [Complete Code Example](https://github.com/openbase/bco.dal/blob/master/example/src/main/python/org/openbase/bco/dal/example/HowToGivePowerConsumptionColorFeedbackViaRSB.py)

# Contribution
* Feel free to report [new Issues](https://github.com/openbase/bco.dal/issues/new)!
* If you are developer and you want to contribute to BCO
    * Fork the repositories, apply your features or fixes and create pull requests.
    * For long term contribution just apply for an openbase membership via support@openbase.org
