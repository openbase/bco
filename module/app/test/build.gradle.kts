plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.test"))
    api(Testing.junit.jupiter)
    api(Testing.junit.jupiter.api)
}

description = "BCO App Test Framework"
