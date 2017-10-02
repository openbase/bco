#!/bin/bash
mvn  -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.traer.Slf4jMavenTransferListener=warn -Dorg.slf4j.simpleLogger.log.org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry=warn $@
