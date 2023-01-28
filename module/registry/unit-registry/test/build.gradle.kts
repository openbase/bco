plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.util"))
    api(project(":bco.authentication.core"))
    api(project(":bco.authentication.test"))
}

description = "BCO Registry Unit Test"
