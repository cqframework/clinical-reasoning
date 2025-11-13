plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-utility"))
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-cr"))
    api(libs.info.picocli.picocli)
    api(libs.org.slf4j.slf4j.simple)
    api(libs.org.apache.commons.commons.compress)
    api(libs.commons.codec.commons.codec)

    runtimeOnly(libs.ca.uhn.hapi.fhir.hapi.fhir.caching.caffeine)
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (CLI)"
