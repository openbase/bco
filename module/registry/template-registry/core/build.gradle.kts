plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.template.lib"))
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.storage:_")
    api("org.openbase:jul.processing:_")
}

description = "BCO Registry Template Core"
