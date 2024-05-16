plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.unit.lib"))
    api("org.openbase:jul.storage:_")
    api(project(":bco.authentication.lib"))
}

description = "BCO Registry Unit Remote"
