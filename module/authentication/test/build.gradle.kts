plugins {
    id("org.openbase.bco")
}

configurations {

}

dependencies {
    api(project(":bco.authentication.core"))
    api(project(":bco.authentication.lib"))
    api("org.testcontainers:junit-jupiter:_") {
        exclude(group = "junit", module = "junit")
    }
    api("io.quarkus:quarkus-junit4-mock:_")
    api("org.openbase:jul.communication.mqtt.test:_")
    api(Testing.junit.jupiter)
    api(Testing.junit.jupiter.api)
}

description = "BCO Authentication Test"
