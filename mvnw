#!/bin/bash
mvn \
-q  \
-B  \
-Dmaven.javadoc.skip=true \
-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B \
-Dorg.slf4j.simpleLogger.log.org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry=warn \
$@
