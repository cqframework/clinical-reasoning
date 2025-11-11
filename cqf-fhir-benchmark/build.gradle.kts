plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-cr"))
    api(project(":cqf-fhir-test"))
    api(libs.org.openjdk.jmh.jmh.core)
    testImplementation(libs.org.skyscreamer.jsonassert)
    testImplementation(libs.org.slf4j.slf4j.simple)
}

description = "FHIR Clinical Reasoning (Benchmarks)"
