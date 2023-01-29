plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.control"))
    testImplementation(project(":bco.app.test"))
}

description = "BCO App Preset"
