#!/bin/bash
set -ev
echo ### start deployment...
mvn deploy -Pdeploy,sonatype --settings .travis/settings.xml -DskipTests=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B
echo ### deployment successfully finished
