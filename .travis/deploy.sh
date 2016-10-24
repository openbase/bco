#!/bin/bash
set -ev
mvn clean deploy -Pdeploy,sonatype --settings .travis/settings.xml -DskipTests=true -B -U
