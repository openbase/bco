/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    api(project(":bco.registry.message.lib"))
    api("org.openbase:jul.extension.rsb.scope:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.type.processing:2.0-SNAPSHOT")
    api("org.openbase:jul.extension.type.storage:2.0-SNAPSHOT")
    api("org.openbase:jul.processing:2.0-SNAPSHOT")
    api(project(":bco.authentication.core"))
}

description = "BCO Registry Message Core"

