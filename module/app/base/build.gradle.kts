plugins {
    id("org.openbase.bco")
    application
}

repositories {
    maven {
        url = uri("https://openhab.jfrog.io/openhab/libs-release")
    }
}

application {
    mainClass.set("org.openbase.bco.app.util.launch.BCOLauncher")
}

distributions {
    main {
        distributionBaseName.set("bco")
    }
}

application.applicationName = "bco"

dependencies {
    api(project(":bco.registry.util"))
    api(project(":bco.device.openhab"))
    api(project(":bco.app.manager"))
    api(project(":bco.dal.control"))
    api(project(":bco.app.cloud.connector"))
    api(project(":bco.app.influxdb.connector"))
    api(project(":bco.api.graphql"))
    api("org.apache.commons:commons-collections4:_")
    testImplementation(project(":bco.dal.test"))
}

description = "BCO App Base"
