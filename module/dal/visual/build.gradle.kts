plugins {
    id("org.openbase.bco")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "21.0.1"
    modules = listOf(
        "javafx.base",
        "javafx.graphics",
        "javafx.media",
        "javafx.controls",
        "javafx.fxml"
    )
}

dependencies {
    api(project(":bco.dal.remote"))
    api("org.openbase:jul.visual.javafx:_")
    api("org.openbase:jul.visual.swing:_")
}

description = "BCO DAL Visualisation"
