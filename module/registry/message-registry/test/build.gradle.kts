plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.util"))
    api(project(":bco.authentication.core"))
}

description = "BCO Registry Message Test"
