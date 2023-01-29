plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.message.lib"))
    api(project(":bco.registry.unit.remote"))
    api("org.openbase:jul.storage:_")
    api(project(":bco.authentication.lib"))
}

description = "BCO Registry Message Remote"
