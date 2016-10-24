#!/bin/bash
set -ev
echo try to deploy...
mvn clean deploy -Pdeploy,sonatype --settings .travis/settings.xml -DskipTests=true -U #-B
echo deployment ok
