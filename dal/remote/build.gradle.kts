/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    api(project(":bco.dal.lib"))
    api("org.openbase:bco.registry.util:2.0-SNAPSHOT")
    api("org.openbase:jul.pattern.trigger:2.0-SNAPSHOT")
}

description = "BCO DAL Remote"

