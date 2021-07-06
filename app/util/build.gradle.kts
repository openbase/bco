/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
    application
}

application {
    mainClass.set("org.openbase.bco.app.util.launch.BCOTestLauncher")
}

dependencies {
    api("org.openbase:bco.registry.util:2.0-SNAPSHOT")
    api("org.openbase:bco.device.openhab:2.0-SNAPSHOT")
    api(project(":bco.app.manager"))
    api("org.openbase:bco.dal.control:2.0-SNAPSHOT")
    api(project(":bco.app.cloud.connector"))
    api(project(":bco.app.influxdb.connector"))
    api("org.openbase:bco.api.graphql:2.0-SNAPSHOT")
    api("commons-collections:commons-collections:3.2.2")
    testImplementation("org.openbase:bco.dal.test:2.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:[5.6,5.7-alpha)")
}

description = "BCO App Utility"

