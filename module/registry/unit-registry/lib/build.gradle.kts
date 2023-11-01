plugins {
    id("org.openbase.bco")
}

dependencies {
    api("org.openbase:jul.storage:_")
    api("org.openbase:jul.transformation:_")
    api(project(":bco.registry.lib"))
    api(project(":bco.registry.class.remote"))
    api(project(":bco.registry.template.remote"))
    api(project(":bco.registry.activity.remote"))
    api("org.apache.commons:commons-math3:_")
}

description = "BCO Registry Unit Library"
