/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.unit.lib"))
    api("org.openbase:jul.storage:2.0-SNAPSHOT")
    api(project(":bco.authentication.lib"))
}

description = "BCO Registry Unit Remote"
