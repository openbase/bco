plugins {
    id("org.openbase.bco")
}

dependencies {
    api("org.openbase:jul.storage:_")
    api(project(":bco.registry.lib"))
}

description = "BCO Registry Activity Library"
