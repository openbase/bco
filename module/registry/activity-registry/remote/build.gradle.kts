plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.activity.lib"))
    api(project(":bco.registry.template.remote"))
    api("org.openbase:jul.storage:_")
}

description = "BCO Registry Activity Remote"
