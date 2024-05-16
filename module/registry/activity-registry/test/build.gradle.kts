plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.activity.core"))
    api(project(":bco.registry.activity.remote"))
}

description = "BCO Registry Activity Test"
