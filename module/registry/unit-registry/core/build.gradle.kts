plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.unit.lib"))
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.extension.type.storage:_")
    api("org.openbase:jul.processing:_")
    api(project(":bco.authentication.core"))
}

description = "BCO Registry Unit Core"
