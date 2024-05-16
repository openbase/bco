plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.class.lib"))
    api(project(":bco.registry.template.remote"))
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.extension.type.storage:_")
    api("org.openbase:jul.processing:_")
}

description = "BCO Registry Class Core"
