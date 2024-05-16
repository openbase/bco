plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.registry.util"))
    api(project(":bco.dal.remote"))
    api(project(":bco.dal.control"))
    api(project(":bco.dal.visual"))
    api(project(":bco.authentication.test"))
    api(Testing.junit.jupiter)
    api(Testing.junit.jupiter.api)
}

description = "BCO DAL Test"
