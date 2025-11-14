plugins {
    id("buildlogic.java-conventions")
}

val hapiVersion = project.property("hapi.version")!!.toString()

dependencies {
    api("org.hamcrest:hamcrest-all:1.3")
    api("org.mockito:mockito-core:5.5.0")
    api("org.mockito:mockito-junit-jupiter:5.5.0")

    // Missing from hapi-bom, hence the need for a version here
    api("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:${hapiVersion}")

    api("org.junit.jupiter:junit-jupiter")
    runtimeOnly("org.junit.platform:junit-platform-launcher")
}

description = "FHIR Clinical Reasoning (Test Utilities)"
