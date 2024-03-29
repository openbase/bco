plugins {
    id("org.openbase.bco")
}

description = "BCO Authentication Library"


dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
    api("org.openbase:jul.communication.mqtt:_")
    api("org.openbase:jul.extension.type.processing:_")
    api("org.openbase:jul.extension.protobuf:_")
    api("org.openbase:jul.pattern.launch:_")
}
