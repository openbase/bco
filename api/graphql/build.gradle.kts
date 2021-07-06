
plugins {
    id("org.openbase.java-conventions")
}

dependencies {
    implementation("com.graphql-java-kickstart:graphql-spring-boot-starter:8.0.0")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("org.springframework.boot:spring-boot-starter-jetty:2.4.0")
    implementation("org.springframework:spring-webflux:5.3.1")
    implementation("com.google.api.graphql:rejoiner:0.3.1")
    implementation("com.google.guava:guava:28.0-jre")
    implementation("net.javacrumbs.future-converter:future-converter-java8-guava:1.2.0")
    implementation("org.openbase:bco.dal.remote:2.0-SNAPSHOT")
    implementation("org.jmdns:jmdns:3.5.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.20")
}

description = "BCO GraphQL API"
