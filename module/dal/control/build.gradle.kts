plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.lib"))
    api(project(":bco.dal.remote"))
    api(project(":bco.registry.util"))
    api("org.openbase:jul.pattern.trigger:_")
    api("com.influxdb:influxdb-client-java:_")
}

description = "BCO DAL Control"
