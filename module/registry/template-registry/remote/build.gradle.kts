plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.template.lib"))
    api("org.openbase:jul.storage:_")
}

description = "BCO Registry Template Remote"
