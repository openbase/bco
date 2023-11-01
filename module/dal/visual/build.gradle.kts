plugins {
    id("org.openbase.bco")
}

dependencies {
    api(project(":bco.dal.remote"))
    api("org.openbase:jul.visual.javafx:_")
    api("org.openbase:jul.visual.swing:_")
}

description = "BCO DAL Visualisation"
