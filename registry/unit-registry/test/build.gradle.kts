/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    api(project(":bco.registry.util"))
    api(project(":bco.authentication.core"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:[5.6,5.7-alpha)")
    testImplementation("org.junit.vintage:junit-vintage-engine:[5.6,5.7-alpha)")
}

description = "BCO Registry Unit Test"

