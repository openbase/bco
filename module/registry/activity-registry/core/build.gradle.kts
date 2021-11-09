/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.activity.lib"))
    api(project(":bco.registry.template.remote"))
    api("org.openbase:jul.extension.type.processing:2.0-SNAPSHOT")
    api("org.openbase:jul.storage:2.0-SNAPSHOT")
    api("org.openbase:jul.processing:2.0-SNAPSHOT")
}

description = "BCO Registry Activity Core"

