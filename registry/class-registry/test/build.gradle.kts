/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.class.core"))
    api(project(":bco.registry.class.remote"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:[5.6,5.7-alpha)")
    testImplementation("org.junit.vintage:junit-vintage-engine:[5.6,5.7-alpha)")
}

description = "BCO Registry Class Test"

