plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.message.lib"))
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.extension.type.storage:_")
    api("org.openbase:jul.processing:_")
    api(project(":bco.authentication.core"))
}

description = "BCO Registry Message Core"
