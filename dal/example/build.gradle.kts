/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    implementation(project(":bco.dal.remote"))
}

description = "BCO DAL Example"

java {
    withJavadocJar()
}
