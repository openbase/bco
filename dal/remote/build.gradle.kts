/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.lib"))
    api(project(":bco.registry.util"))
    api("org.openbase:jul.pattern.trigger:2.0-SNAPSHOT")
}

description = "BCO DAL Remote"

