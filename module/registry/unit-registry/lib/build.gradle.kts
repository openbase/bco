/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api("org.openbase:jul.storage:_")
    api("org.openbase:rct:[2.0,2.1-alpha)")
    api(project(":bco.registry.lib"))
    api(project(":bco.registry.class.remote"))
    api(project(":bco.registry.template.remote"))
    api(project(":bco.registry.activity.remote"))
    api("org.apache.commons:commons-math3:3.6.1")
}

description = "BCO Registry Unit Library"

