plugins {
    id("org.openbase.bco")
    application
}

repositories {
    maven {
        url = uri("https://openhab.jfrog.io/openhab/libs-release")
    }
}

application {
    mainClass.set("org.openbase.bco.device.openhab.OpenHABDeviceManagerLauncher")
}

dependencies {
    api(project(":bco.dal.control"))
    api("org.glassfish.jersey.core:jersey-client:_")
    api("org.glassfish.jersey.inject:jersey-hk2:_")
    api("org.glassfish.jersey.media:jersey-media-sse:_")
    api("org.glassfish.jersey.security:oauth2-client:_")
    api("org.openhab.core.bundles:org.openhab.core.io.rest.core:_")
}

description = "BCO Openhab Device Manager"
