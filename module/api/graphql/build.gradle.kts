plugins {
    id("org.openbase.bco")
    id("org.springframework.boot")
    id("io.spring.dependency-management") version "1.1.2"
}

dependencies {

    api(project(":bco.dal.remote"))
    api("org.springframework.boot:spring-boot-starter-webflux:_")
    api("com.graphql-java-kickstart:graphql-spring-boot-starter:_") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    api("org.springframework.boot:spring-boot-starter-jetty:_")
    api("org.springframework.boot:spring-boot-starter-websocket:_") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    api(Spring.boot.webflux)
    api("org.springframework:spring-webmvc:_")

    api("org.eclipse.jetty:jetty-server:11.0.14")
    api("jakarta.servlet:jakarta.servlet-api:6.0.0")



    api(rootProject.files("lib/external/rejoiner-0.5.0-bco.jar"))
    api(rootProject.files("lib/external/rejoiner-guice-0.5.0-bco.jar"))
// disabled since rejoiner is linked locally.
//    api("com.google.api.graphql:rejoiner-guice:_") {
//        exclude(group = "com.google.inject", module = "guice")
//        exclude(group = "com.graphql-java", module = "graphql-java")
//        exclude(group = "com.google.inject.extensions", module = "guice-multibindings")
//    }
//    api("com.google.api.graphql:rejoiner:_") {
//        exclude(group = "com.google.inject", module = "guice")
//        exclude(group = "com.graphql-java", module = "graphql-java")
//        exclude(group = "com.google.inject.extensions", module = "guice-multibindings")
//    }
    api("com.google.inject.extensions:guice-multibindings:_")
    api("com.google.inject:guice:_")
    api("com.google.guava:guava:_")
    api("net.javacrumbs.future-converter:future-converter-java8-guava:_")
    api(ReactiveX.rxJava2)
}

description = "BCO GraphQL API"
