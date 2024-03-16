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
    implementation(project(":bco.dal.control"))
    implementation("jakarta.activation:jakarta.activation-api:2.1.2")
    implementation("org.glassfish.jersey.core:jersey-client:_")
    implementation("org.glassfish.jersey.inject:jersey-hk2:_")
    implementation("org.glassfish.jersey.media:jersey-media-sse:_")
    implementation("org.glassfish.jersey.security:oauth2-client:_")
    implementation("org.openhab.core.bundles:org.openhab.core.io.rest.core:_")
}

description = "BCO Openhab Device Manager"
