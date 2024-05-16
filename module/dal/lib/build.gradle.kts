plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.remote"))
    api(project(":bco.authentication.lib"))
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.extension.protobuf:_")
    api("org.openbase:jul.extension.type.transform:_")
    testImplementation(project(":bco.registry.unit.test"))
}

description = "BCO DAL Library"
