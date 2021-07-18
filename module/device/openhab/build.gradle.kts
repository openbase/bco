/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.openbase.bco")
}

repositories {
    maven {
        url = uri("https://openhab.jfrog.io/openhab/libs-release")
    }
    jcenter()
}

dependencies {
    api(project(":bco.dal.control"))
    api("org.glassfish.jersey.core:jersey-client:2.31")
    api("org.glassfish.jersey.inject:jersey-hk2:2.31")
    api("org.glassfish.jersey.media:jersey-media-sse:2.31")
    api("org.openhab.core.bundles:org.openhab.core.io.rest.core:2.5.0")
}

description = "BCO Openhab Device Manager"
