plugins {
    id("org.openbase.bco")
}

dependencies {
    api("org.openbase:jul.storage:_")
    api("org.openbase:jul.pattern.launch:_")
    api(project(":bco.authentication.lib"))
    api("org.openbase:jul.extension.type.util:_")
    api("org.openbase:jul.communication.controller:_")
    api("org.openbase:jul.exception:_")
}

description = "BCO Registry Lib"
