plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.template.core"))
    api(project(":bco.registry.template.remote"))
}

description = "BCO Registry Template Test"
