plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "org.openbase"

nexusPublishing {
    repositories {
        sonatype {
            username.set(findProperty("MAVEN_CENTRAL_USERNAME")?.let { it as String? })
            password.set(findProperty("MAVEN_CENTRAL_TOKEN")?.let { it as String? })
        }
    }
}
