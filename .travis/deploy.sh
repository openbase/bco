#!/bin/bash
set -ev
echo ### start deployment...
${AUTO_DEPLOY} && mvn deploy -Pdeploy,sonatype --settings .travis/settings.xml -DskipTests=true -B
echo ### deployment successfully finished
