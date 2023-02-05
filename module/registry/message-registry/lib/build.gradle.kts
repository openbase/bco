plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.lib"))
    api(project(":bco.registry.unit.remote"))
}

description = "BCO Registry Message Library"
