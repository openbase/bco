/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    implementation(project(":bco.dal.lib"))
    implementation("org.openbase:bco.registry.util:2.0-SNAPSHOT")
    implementation("org.openbase:jul.pattern.trigger:2.0-SNAPSHOT")
}

description = "BCO DAL Remote"

java {
    withJavadocJar()
}
