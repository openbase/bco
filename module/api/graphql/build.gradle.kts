
plugins {
    id("org.openbase.bco")
    //id("org.springframework.boot") version "2.4.0"
    //id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
    api(project(":bco.dal.remote"))
    api("com.graphql-java-kickstart:graphql-spring-boot-starter:8.0.0") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    api("io.reactivex:rxjava:1.3.8")
    api("org.springframework.boot:spring-boot-starter-jetty:2.4.0")
    api("org.springframework:spring-webflux:5.3.1")
    api("com.google.api.graphql:rejoiner:0.3.1") {
        exclude(group = "com.graphql-java", module = "graphql-java")
    }
    api("com.google.guava:guava:28.0-jre")
    api("net.javacrumbs.future-converter:future-converter-java8-guava:1.2.0")
    api("org.jmdns:jmdns:3.5.6")
    api("io.reactivex.rxjava2:rxjava:2.2.20")
}

description = "BCO GraphQL API"
