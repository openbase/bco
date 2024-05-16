plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.control"))
    api(project(":bco.app.preset"))
    api(project(":bco.app.cloud.connector"))
}

description = "BCO App Manager"
