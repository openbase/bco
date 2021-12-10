
plugins {
    id("org.openbase.bco")
    id("org.springframework.boot") version "2.6.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
    api(project(":bco.dal.remote"))
    api("com.graphql-java-kickstart:graphql-spring-boot-starter:8.0.0") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    api("org.springframework.boot:spring-boot-starter-jetty:2.6.1")
    api("org.springframework.boot:spring-boot-starter-webflux:2.6.1")
    api("org.springframework:spring-webmvc:5.3.13")
    api("com.google.api.graphql:rejoiner:0.3.1") {
        exclude(group = "com.google.inject", module = "guice")
        exclude(group = "com.graphql-java", module = "graphql-java")
        exclude(group = "com.google.inject.extensions", module = "guice-multibindings")
    }
    api("com.google.inject.extensions:guice-multibindings:4.2.3")
    api("com.google.inject:guice:5.0.1")
    api("com.google.guava:guava:28.0-jre")
    api("net.javacrumbs.future-converter:future-converter-java8-guava:1.2.0")
    api("org.jmdns:jmdns:3.5.6")
    api("io.reactivex.rxjava2:rxjava:2.2.21")
}

description = "BCO GraphQL API"
