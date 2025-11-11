plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-api"))
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.caching.caffeine)
    testImplementation(libs.org.skyscreamer.jsonassert)
}

description = "FHIR Clinical Reasoning (Test Utilities)"
