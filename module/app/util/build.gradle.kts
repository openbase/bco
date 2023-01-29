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

fun createAdditionalScript(name: String, configureStartScripts: CreateStartScripts.() -> Unit) =
    tasks.register<CreateStartScripts>("startScripts$name") {
        configureStartScripts()
        applicationName = name
        outputDir = File(project.buildDir, "scripts")
        classpath = tasks.getByName("jar").outputs.files + configurations.runtimeClasspath.get()
    }.also {
        application.applicationDistribution.into("bin") {
            from(it)
            //fileMode = 0b000_111_101_101
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

createAdditionalScript("bco-logo") {
    mainClass.set("org.openbase.bco.app.util.launch.LogoPrinter")
}

createAdditionalScript("bco-ping") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOPing")
}

createAdditionalScript("bco-manager") {
    mainClass.set("org.openbase.bco.app.util.launch.ManagerLauncher")
}

createAdditionalScript("bco-manager-device-openhab") {
    mainClass.set("org.openbase.bco.device.openhab.OpenHABDeviceManagerLauncher")
}

createAdditionalScript("bco-test") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOTestLauncher")
}

createAdditionalScript("bco-print-api") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOInterfacePrinter")
}

createAdditionalScript("bco-stats") {
    mainClass.set("org.openbase.bco.registry.print.BCORegistryPrinter")
}

createAdditionalScript("bco-validate") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOSystemValidator")
}

createAdditionalScript("bco-registry-validate") {
    mainClass.set("org.openbase.bco.app.util.launch.BCORegistryValidator")
}

createAdditionalScript("bco-registry") {
    mainClass.set("org.openbase.bco.registry.launch.RegistryLauncher")
}

createAdditionalScript("bco-app-adhoc-generate-trainingdata") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOAdhocTrainDataGeneratorLauncher")
}

createAdditionalScript("bco-console") {
    mainClass.set("org.openbase.bco.app.util.launch.BCOConsole")
}

createAdditionalScript("bco-visual-remote") {
    mainClass.set("org.openbase.bco.dal.visual.BCOVisualRemote")
}

createAdditionalScript("bco-query") {
    mainClass.set("org.openbase.bco.registry.print.BCOUnitQueryPrinter")
}

createAdditionalScript("bco-query-label") {
    mainClass.set("org.openbase.bco.registry.print.BCOQueryLabelPrinter")
}

createAdditionalScript("bco-logger") {
    mainClass.set("org.openbase.bco.dal.remote.printer.BCOLogger")
}

createAdditionalScript("bco-actions") {
    mainClass.set("org.openbase.bco.dal.visual.action.BCOActionInspectorLauncher")
}

distributions {
    main {
        distributionBaseName.set("bco")
    }
}

tasks.register("deploy-bco-dist") {
    dependsOn("installDist")
    val bcoDist = System.getenv("BCO_DIST") ?: "${System.getenv("HOME")}/usr"
    val mainDist = distributions.getByName("main").distributionBaseName.get()
    val fromDir = File(project.buildDir, "install/$mainDist")
    doFirst {
        copy {
            from(fromDir)
            into(bcoDist)
        }
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
    api(project(":bco.dal.visual"))
    api("commons-collections:commons-collections:_")
    testImplementation(project(":bco.dal.test"))
}

description = "BCO App Utility"

tasks.register("testDeploy") {
    dependsOn("installDist")
    //println("Copy to ${System.getenv("HOME")}/local/bco_tmp")
    //println("BCO_DIST: ${BCO_DIST}")
    copy {
        from(File(project.buildDir, "install/bco-test"))
        into("${System.getenv("HOME")}/local/bco_tmp")
    }
}
