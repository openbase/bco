plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.lib"))
    api(project(":bco.registry.util"))
    api("org.openbase:jul.pattern.trigger:_")
}

description = "BCO DAL Remote"
