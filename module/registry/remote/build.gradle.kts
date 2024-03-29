plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.lib"))
    api(project(":bco.registry.activity.remote"))
    api(project(":bco.registry.class.remote"))
    api(project(":bco.registry.template.remote"))
    api(project(":bco.registry.unit.remote"))
    api(project(":bco.registry.message.remote"))
}

description = "BCO Registry Remote"
