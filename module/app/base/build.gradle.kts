plugins {
    id("org.openbase.bco")
//    id("org.graalvm.buildtools.native")
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

//nativeBuild {
//    imageName = "bco"
//    mainClass = "org.openbase.bco.app.util.launch.BCOLauncher"
//    verbose = true
//    fallback = false
//}

//nativeImage {
//    graalVmHome = System.getenv("JAVA_HOME")
//    buildType { build ->
//        build.executable(main = 'org.openbase.bco.app.util.launch.BCOLauncher')
//    }
//    executableName = "bco"
//    outputDirectory = file("$buildDir/executable")
//    arguments(
//        "--no-fallback",
//        "--enable-all-security-services",
//        options.traceClassInitialization('org.openbase.bco.app.util.launch.BCOLauncher'),
//        "--initialize-at-run-time=com.example.runtime",
//        "--report-unsupported-elements-at-runtime"
//    )
//}

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
