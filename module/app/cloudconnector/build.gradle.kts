/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.control"))
    api("io.socket:socket.io-client:_")
    testImplementation(project(":bco.authentication.test"))
}

description = "BCO Cloud Connector"

