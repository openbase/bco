/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://mvn.cit-ec.de/nexus/content/repositories/releases/")
    }

    maven {
        url = uri("https://mvn.cit-ec.de/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("org.openbase:jul.extension.rsb.scope:2.0-SNAPSHOT")
    implementation("org.openbase:jul.extension.type.processing:2.0-SNAPSHOT")
    implementation("org.openbase:jul.extension.protobuf:2.0-SNAPSHOT")
    implementation("org.openbase:jul.pattern.launch:2.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:[5.6,5.7-alpha)")
    testImplementation("org.junit.vintage:junit-vintage-engine:[5.6,5.7-alpha)")
}

group = "org.openbase"
version = "2.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

java {
    withSourcesJar()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
