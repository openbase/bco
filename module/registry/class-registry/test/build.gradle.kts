plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.class.core"))
    api(project(":bco.registry.class.remote"))
}

description = "BCO Registry Class Test"
