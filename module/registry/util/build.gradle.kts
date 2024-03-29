plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.lib"))
    api(project(":bco.registry.activity.core"))
    api(project(":bco.registry.class.core"))
    api(project(":bco.registry.template.core"))
    api(project(":bco.registry.unit.core"))
    api(project(":bco.registry.message.core"))
    api(project(":bco.registry.remote"))
    testImplementation(project(":bco.authentication.test"))
}

description = "BCO Registry Utility"
