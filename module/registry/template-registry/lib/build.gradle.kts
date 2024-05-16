plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.lib"))
    api("org.openbase:jul.storage:_")
}

description = "BCO Registry Template Library"
