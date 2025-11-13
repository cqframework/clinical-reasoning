plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api("org.hamcrest:hamcrest-all:1.3")
    api("org.mockito:mockito-core:5.5.0")
    api("nl.jqno.equalsverifier:equalsverifier:3.15.6")
    api("org.junit.jupiter:junit-jupiter")
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.caching.caffeine)
    api(libs.org.skyscreamer.jsonassert)
    api(libs.org.mockito.mockito.junit.jupiter)
    api(libs.org.openjdk.jmh.jmh.core)
    api(libs.com.github.valfirst.slf4j.test)
    runtimeOnly("org.junit.platform:junit-platform-launcher")
}

description = "FHIR Clinical Reasoning (Test Utilities)"
