/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.remote"))
    api(project(":bco.authentication.lib"))
    api("org.openbase:jul.extension.rsb.scope:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.rsb.com:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.rsb.processing:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.type.processing:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.rsb.interface:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.protobuf:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.type.transform:2.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:[5.6,5.7-alpha)")
    testImplementation(project(":bco.registry.unit.test"))
}

description = "BCO DAL Library"
