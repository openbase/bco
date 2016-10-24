#!/bin/bash
APP_NAME='bco.registry'
clear &&
echo "=== clean ${APP_NAME} ===" &&
mvn clean $@ &&
clear &&
echo "=== deploy ${APP_NAME} to ${prefix} ===" &&
mvn install -DskipTests -DassembleDirectory=${prefix} $@ &&
clear &&
echo "=== ${APP_NAME} is successfully installed to ${prefix} ==="
